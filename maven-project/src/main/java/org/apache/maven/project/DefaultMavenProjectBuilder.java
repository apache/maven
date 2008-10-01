package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.MavenTools;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.builder.PomArtifactResolver;
import org.apache.maven.project.builder.ProjectBuilder;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.project.builder.PomClassicTransformer;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.text.SimpleDateFormat;


/**
 * @version $Id$
 */
public class DefaultMavenProjectBuilder
    implements MavenProjectBuilder, Initializable, LogEnabled
{
    protected MavenProfilesBuilder profilesBuilder;

    protected ArtifactResolver artifactResolver;

    protected ArtifactMetadataSource artifactMetadataSource;

    private ArtifactFactory artifactFactory;

    private ModelValidator validator;

    // TODO: make it a component
    private MavenXpp3Reader modelReader;

    private ProfileAdvisor profileAdvisor;

    private MavenTools mavenTools;

    private ProjectBuilder projectBuilder;

    private RepositoryHelper repositoryHelper;

    private Logger logger;

    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

    // the aspect weaving seems not to work for reflection from plugin.

    private Map processedProjectCache = new HashMap();

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    private static HashMap<String, MavenProject> hm = new HashMap<String, MavenProject>();    
    
    public void initialize()
    {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    // This is used by the SITE plugin.
    public MavenProject build( File project, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration cbf = new DefaultProjectBuilderConfiguration();
        cbf.setLocalRepository( localRepository );
        cbf.setGlobalProfileManager( profileManager );
        return build( project, cbf );
    }    
    
    public MavenProject build( File projectDescriptor, ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
            MavenProject project = readModelFromLocalPath( "unknown", projectDescriptor, new PomArtifactResolver(
                config.getLocalRepository(), repositoryHelper.buildArtifactRepositories(
                getSuperProject( config, projectDescriptor, true ).getModel() ), artifactResolver ), config );

            project.setFile( projectDescriptor );
            project = buildInternal( project.getModel(), config, projectDescriptor, project.getParentFile(), true );

            Build build = project.getBuild();
            // NOTE: setting this script-source root before path translation, because
            // the plugin tools compose basedir and scriptSourceRoot into a single file.
            project.addScriptSourceRoot( build.getScriptSourceDirectory() );
            project.addCompileSourceRoot( build.getSourceDirectory() );
            project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
            project.setFile( projectDescriptor );

            setBuildOutputDirectoryOnParent( project );
        return project;
    }

    // I want to build this out as a component with history and statistics to help me track down the realm problems. jvz.
    class ProjectCache
    {
        private Map<String, MavenProject> projects = new HashMap<String, MavenProject>();
        
        public MavenProject get( String key )
        {
            MavenProject p = projects.get( key ); 
                        
            return p;            
        }
        
        public MavenProject put( String key, MavenProject project )
        {
            return projects.put( key, project );
        }
    }
    
    // This is used by the RR plugin
    public MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository, boolean allowStubs )
        throws ProjectBuildingException
    {
        return buildFromRepository( artifact, remoteArtifactRepositories, localRepository );
    }

    public MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        MavenProject project = hm.get( artifact.getId() );
        
        if ( project != null )
        {            
            return project;
        }        
        
        File f = (artifact.getFile() != null) ? artifact.getFile() :
                new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );;
        repositoryHelper.findModelFromRepository( artifact, remoteArtifactRepositories, localRepository );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );

        List<ArtifactRepository> artifactRepositories = new ArrayList<ArtifactRepository>( remoteArtifactRepositories );
        artifactRepositories.addAll( repositoryHelper.buildArtifactRepositories( getSuperProject( config, artifact.getFile(), false ).getModel() ) );

        project = readModelFromLocalPath( "unknown", artifact.getFile(), new PomArtifactResolver( config.getLocalRepository(), artifactRepositories, artifactResolver ), config );
        project = buildInternal( project.getModel(), config, artifact.getFile(), project.getParentFile(), false );

        artifact.setFile( f );
        project.setVersion( artifact.getVersion() );

        hm.put( artifact.getId(), project );
        
        return project;
    }

    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        superModel.setGroupId( STANDALONE_SUPERPOM_GROUPID );

        superModel.setArtifactId( STANDALONE_SUPERPOM_ARTIFACTID );

        superModel.setVersion( STANDALONE_SUPERPOM_VERSION );

        superModel = superModel;

        ProfileManager profileManager = config.getGlobalProfileManager();

        List activeProfiles = new ArrayList();
        if ( profileManager != null )
        {
            List activated = profileAdvisor.applyActivatedProfiles( superModel, null, false,
                                                                    profileManager.getProfileActivationContext() );
            if ( !activated.isEmpty() )
            {
                activeProfiles.addAll( activated );
            }

            activated = profileAdvisor.applyActivatedExternalProfiles( superModel, null, profileManager );
            if ( !activated.isEmpty() )
            {
                activeProfiles.addAll( activated );
            }
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getExecutionProperties(),
                PomInterpolatorTag.SYSTEM_PROPERTIES.name()));
        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getUserProperties(),
                PomInterpolatorTag.USER_PROPERTIES.name()));

        if(config.getBuildStartTime() != null)
        {
            interpolatorProperties.add(new InterpolatorProperty("${build.timestamp}",
                new SimpleDateFormat("yyyyMMdd-hhmm").format( config.getBuildStartTime() ),
                PomInterpolatorTag.PROJECT_PROPERTIES.name()));
        }

        File basedir = null;
        for(InterpolatorProperty ip : interpolatorProperties )
        {
            if(ip.getKey().equals("${basedir}"))
            {
                basedir = new File(ip.getValue());
                break;
            }
        }

        if(basedir == null)
        {
            String bd = System.getProperty("basedir");
            if( bd != null )
            {
                basedir = new File(bd);
            }
        }

        try
        {
            superModel = PomClassicTransformer.interpolateModel(superModel, interpolatorProperties, basedir);
        }
        catch (IOException e)
        {
            throw new ProjectBuildingException(STANDALONE_SUPERPOM_GROUPID + ":" + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Interpolation failure:", e);
        }

        MavenProject project;
        try
        {
            project = new MavenProject( superModel, artifactFactory, mavenTools, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":" + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid repository!", e );
        }

        getLogger().debug( "Activated the following profiles for standalone super-pom: " + activeProfiles );

        try
        {
            project = constructMavenProjectFromModel( project.getModel(), null, null, config );
            project.setActiveProfiles( activeProfiles );
            project.setRemoteArtifactRepositories(
                mavenTools.buildArtifactRepositories( superModel.getRepositories() ) );
            project.setPluginArtifactRepositories(
                mavenTools.buildArtifactRepositories( superModel.getRepositories() ) );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":" + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid repository!", e );
        }

        project.setExecutionRoot( true );

        return project;
    }

    public MavenProjectBuildingResult buildProjectWithDependencies( File projectDescriptor,
                                                                    ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        MavenProject project = build( projectDescriptor, config );

        try
        {
            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ),
                                                "Unable to build project due to an invalid dependency version: " +
                                                    e.getMessage(), projectDescriptor, e );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( project.getArtifact() )
            .setArtifactDependencies( project.getDependencyArtifacts() )
            .setLocalRepository( config.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() )
            .setMetadataSource( artifactMetadataSource );

        ArtifactResolutionResult result = artifactResolver.resolve( request );

        project.setArtifacts( result.getArtifacts() );

        return new MavenProjectBuildingResult( project, result );
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private Logger getLogger()
    {
        return logger;
    }

    private MavenProject buildInternal( Model model, ProjectBuilderConfiguration config, File projectDescriptor,
                                        File parentDescriptor, boolean isReactorProject )
        throws ProjectBuildingException
    {
        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        ProfileActivationContext profileActivationContext;
        ProfileManager externalProfileManager = config.getGlobalProfileManager();
        if ( externalProfileManager != null )
        {
            // used to trigger the caching of SystemProperties in the container context...
            try
            {
                externalProfileManager.getActiveProfiles();
            }
            catch ( ProfileActivationException e )
            {
                throw new ProjectBuildingException( projectId, "Failed to activate external profiles.",
                                                    projectDescriptor, e );
            }
            profileActivationContext = externalProfileManager.getProfileActivationContext();
        }
        else
        {
            profileActivationContext = new DefaultProfileActivationContext( config.getExecutionProperties(), false );
        }

        MavenProject project;
        try
        {
            project = constructMavenProjectFromModel( model, projectDescriptor, parentDescriptor, config );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
        }

        List<Profile> projectProfiles = new ArrayList<Profile>();
        projectProfiles.addAll( profileAdvisor.applyActivatedProfiles( project.getModel(), project.getFile(),
                                                                       isReactorProject, profileActivationContext ) );
        projectProfiles.addAll( profileAdvisor.applyActivatedExternalProfiles( project.getModel(), project.getFile(),
                                                                               externalProfileManager ) );
        project.setActiveProfiles( projectProfiles );

        return project;
    }

    private MavenProject constructMavenProjectFromModel( Model model, File pomFile, File parentFile,
                                                           ProjectBuilderConfiguration config )
            throws ProjectBuildingException, InvalidRepositoryException
    {

        MavenProject project = new MavenProject( model, artifactFactory, mavenTools, this, config );
        validateModel( model, pomFile );

        Artifact projectArtifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                                        project.getVersion(), project.getPackaging() );
        project.setArtifact( projectArtifact );
        project.setParentFile( parentFile );

        return project;
    }

    private MavenProject getSuperProject( ProjectBuilderConfiguration config, File projectDescriptor,
                                          boolean isReactorProject )
        throws ProjectBuildingException
    {

        MavenProject superProject;
        Model model = getSuperModel();
        try
        {
            superProject = new MavenProject( model, artifactFactory, mavenTools, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":" + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid repository!", e );
        }

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        ProfileActivationContext profileActivationContext;
        ProfileManager externalProfileManager = config.getGlobalProfileManager();
        if ( externalProfileManager != null )
        {
            // used to trigger the caching of SystemProperties in the container context...
            try
            {
                externalProfileManager.getActiveProfiles();
            }
            catch ( ProfileActivationException e )
            {
                throw new ProjectBuildingException( projectId, "Failed to activate external profiles.",
                                                    projectDescriptor, e );
            }
            profileActivationContext = externalProfileManager.getProfileActivationContext();
        }
        else
        {
            profileActivationContext = new DefaultProfileActivationContext( config.getExecutionProperties(), false );
        }

        List<Profile> superProjectProfiles = new ArrayList<Profile>();
        superProjectProfiles.addAll( profileAdvisor.applyActivatedProfiles( model, projectDescriptor, isReactorProject,
                                                                            profileActivationContext ) );
        superProjectProfiles.addAll(
            profileAdvisor.applyActivatedExternalProfiles( model, projectDescriptor, externalProfileManager ) );
        superProject.setActiveProfiles( superProjectProfiles );

        return superProject;
    }

    private Model superModel;

    private Model getSuperModel()
        throws ProjectBuildingException
    {
        if ( superModel != null )
        {
            return superModel;
        }

        URL url = DefaultMavenProjectBuilder.class.getResource( "pom-" + MAVEN_MODEL_VERSION + ".xml" );
        String projectId = safeVersionlessKey( STANDALONE_SUPERPOM_GROUPID, STANDALONE_SUPERPOM_ARTIFACTID );

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( url.openStream() );
            String modelSource = IOUtil.toString( reader );

            if ( modelSource.indexOf( "<modelVersion>" + MAVEN_MODEL_VERSION ) < 0 )
            {
                throw new InvalidProjectModelException( projectId, "Not a v" + MAVEN_MODEL_VERSION + " POM.",
                                                        new File( "." ) );
            }

            StringReader sReader = new StringReader( modelSource );

            superModel = modelReader.read( sReader, STRICT_MODEL_PARSING );
            return superModel;
        }
        catch ( XmlPullParserException e )
        {
            throw new InvalidProjectModelException( projectId, "Parse error reading POM. Reason: " + e.getMessage(),
                                                    e );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "Failed build model from URL \'" + url.toExternalForm() +
                "\'\nError: \'" + e.getLocalizedMessage() + "\'", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private MavenProject readModelFromLocalPath( String projectId, File projectDescriptor, PomArtifactResolver resolver,
                                                 ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        if ( projectDescriptor == null )
        {
            throw new IllegalArgumentException( "projectDescriptor: null, Project Id =" + projectId );
        }

        if ( projectBuilder == null )
        {
            throw new IllegalArgumentException( "projectBuilder: not initialized" );
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getExecutionProperties(), 
                PomInterpolatorTag.SYSTEM_PROPERTIES.name()));
        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getUserProperties(),
                PomInterpolatorTag.USER_PROPERTIES.name()));

        if(config.getBuildStartTime() != null)
        {
            interpolatorProperties.add(new InterpolatorProperty("${build.timestamp}",
                new SimpleDateFormat("yyyyMMdd-hhmm").format( config.getBuildStartTime() ),
                PomInterpolatorTag.PROJECT_PROPERTIES.name()));
        }

        MavenProject mavenProject;
        try
        {
            mavenProject = projectBuilder.buildFromLocalPath( new FileInputStream( projectDescriptor ), Arrays.asList(
                getSuperProject( config, projectDescriptor, true ).getModel() ), null, interpolatorProperties, resolver,
                                                                                 projectDescriptor.getParentFile(),
                                                                                 config );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "File = " + projectDescriptor.getAbsolutePath(), e );
        }

        return mavenProject;

    }

    private void validateModel( Model model, File pomFile )
        throws InvalidProjectModelException
    {
        // Must validate before artifact construction to make sure dependencies are good
        ModelValidationResult validationResult = validator.validate( model );

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        if ( validationResult.getMessageCount() > 0 )
        {
            for ( String s : (List<String>) validationResult.getMessages() )
            {
                System.out.println( s );
            }
            throw new InvalidProjectModelException( projectId, "Failed to validate POM", pomFile, validationResult );
        }
    }

    private static String safeVersionlessKey( String groupId, String artifactId )
    {
        String gid = groupId;

        if ( StringUtils.isEmpty( gid ) )
        {
            gid = "unknown";
        }

        String aid = artifactId;

        if ( StringUtils.isEmpty( aid ) )
        {
            aid = "unknown";
        }

        return ArtifactUtils.versionlessKey( gid, aid );
    }

    private static void setBuildOutputDirectoryOnParent( MavenProject project )
    {
        MavenProject parent = project.getParent();
        if ( parent != null )
        {
            parent.getModel().getBuild().setDirectory( parent.getFile().getAbsolutePath() );
            setBuildOutputDirectoryOnParent( parent );
        }
    }
}