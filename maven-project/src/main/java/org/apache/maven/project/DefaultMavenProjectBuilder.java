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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.builder.PomArtifactResolver;
import org.apache.maven.project.builder.ProjectBuilder;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.project.workspace.ProjectWorkspace;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*:apt

 -----
 POM lifecycle
 -----

POM Lifecycle

 Order of operations when building a POM

 * inheritance
 * path translation
 * interpolation
 * defaults injection

 Current processing is:

 * inheritance
 * interpolation
 * defaults injection
 * path translation

 I'm not sure how this is working at all ... i think i have a case where this is failing but i need to
 encapsulate as a test so i can fix it. Also need to think of the in working build directory versus looking
 things up from the repository i.e buildFromSource vs buildFromRepository.

Notes

 * when the model is read it may not have a groupId, as it must be inherited

 * the inheritance assembler must use models that are unadulterated!

*/

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

    private PathTranslator pathTranslator;

    private ModelInterpolator modelInterpolator;

    private ProfileAdvisor profileAdvisor;

    private MavenTools mavenTools;

    private ProjectWorkspace projectWorkspace;

    private ProjectBuilder projectBuilder;

    private RepositoryHelper repositoryHelper;

    private Logger logger;

    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

    // the aspect weaving seems not to work for reflection from plugin.

    private Map processedProjectCache = new HashMap();

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    public void initialize()
    {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository,
                               ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration config =
            new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository )
                .setGlobalProfileManager( profileManager );

        return build( projectDescriptor, config );
    }

    public MavenProject build( File projectDescriptor, ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        MavenProject project = projectWorkspace.getProject( projectDescriptor );

        if ( project == null )
        {
            project = readModelFromLocalPath( "unknown", projectDescriptor, new PomArtifactResolver(
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

        }
        return project;
    }


    /**
     * @deprecated
     */
    @Deprecated
    public MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                             ArtifactRepository localRepository, boolean allowStub )
        throws ProjectBuildingException

    {
        return buildFromRepository( artifact, remoteArtifactRepositories, localRepository );
    }


    public MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                             ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        MavenProject project = null;
        if ( !Artifact.LATEST_VERSION.equals( artifact.getVersion() ) &&
            !Artifact.RELEASE_VERSION.equals( artifact.getVersion() ) )
        {
            project =
                projectWorkspace.getProject( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }
        File f = artifact.getFile();
        if ( project == null )
        {
            repositoryHelper.findModelFromRepository( artifact, remoteArtifactRepositories, localRepository );

            ProjectBuilderConfiguration config =
                new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );

            List<ArtifactRepository> artifactRepositories =
                new ArrayList<ArtifactRepository>( remoteArtifactRepositories );
            artifactRepositories.addAll( repositoryHelper.buildArtifactRepositories(
                getSuperProject( config, artifact.getFile(), false ).getModel() ) );

            project = readModelFromLocalPath( "unknown", artifact.getFile(), new PomArtifactResolver(
                config.getLocalRepository(), artifactRepositories, artifactResolver ), config );
            project = buildInternal( project.getModel(), config, artifact.getFile(), project.getParentFile(), false );
        }

        artifact.setFile( f );
        project.setVersion( artifact.getVersion() );

        return project;
    }

    // what is using this externally? jvz.
    public MavenProject buildStandaloneSuperProject()
        throws ProjectBuildingException
    {
        //TODO mkleint - use the (Container, Properties) constructor to make system properties embeddable
        return buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration() );
    }

    public MavenProject buildStandaloneSuperProject( ProfileManager profileManager )
        throws ProjectBuildingException
    {
        //TODO mkleint - use the (Container, Properties) constructor to make system properties embeddable
        return buildStandaloneSuperProject(
            new DefaultProjectBuilderConfiguration().setGlobalProfileManager( profileManager ) );
    }

    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        superModel.setGroupId( STANDALONE_SUPERPOM_GROUPID );

        superModel.setArtifactId( STANDALONE_SUPERPOM_ARTIFACTID );

        superModel.setVersion( STANDALONE_SUPERPOM_VERSION );

        superModel = ModelUtils.cloneModel( superModel );

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

        MavenProject project;
        try
        {
            project = new MavenProject( superModel, artifactFactory, mavenTools, repositoryHelper, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":" + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid repository!", e );
        }

        getLogger().debug( "Activated the following profiles for standalone super-pom: " + activeProfiles );

        try
        {
            project = interpolateModel( project.getModel(), null, null, config );
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
        catch ( ModelInterpolationException e )
        {
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":" + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid expressions!", e );
        }

        project.setExecutionRoot( true );

        return project;
    }

    /**
     * @since 2.0.x
     */
    public MavenProject buildWithDependencies( File projectDescriptor, ArtifactRepository localRepository,
                                               ProfileManager profileManager )
        throws ProjectBuildingException
    {
        return buildProjectWithDependencies( projectDescriptor, localRepository, profileManager ).getProject();
    }

    /**
     * @since 2.1
     */
    public MavenProjectBuildingResult buildProjectWithDependencies( File projectDescriptor,
                                                                    ArtifactRepository localRepository,
                                                                    ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration config =
            new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository )
                .setGlobalProfileManager( profileManager );

        return buildProjectWithDependencies( projectDescriptor, config );
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

    public void calculateConcreteState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        new MavenProjectRestorer( pathTranslator, modelInterpolator, getLogger() ).calculateConcreteState( project,
                                                                                                           config );
    }

    public void restoreDynamicState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        new MavenProjectRestorer( pathTranslator, modelInterpolator, getLogger() ).restoreDynamicState( project,
                                                                                                        config );
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
            project = interpolateModel( model, projectDescriptor, parentDescriptor, config );
        }
        catch ( ModelInterpolationException e )
        {
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
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

        projectWorkspace.storeProjectByCoordinate( project );
        projectWorkspace.storeProjectByFile( project );

        return project;
    }

    private MavenProject interpolateModel( Model model, File pomFile, File parentFile,
                                                           ProjectBuilderConfiguration config )
        throws ProjectBuildingException, ModelInterpolationException, InvalidRepositoryException
    {
        File projectDir = null;
        if ( pomFile != null )
        {
            projectDir = pomFile.getAbsoluteFile().getParentFile();
        }

        model = modelInterpolator.interpolate( model, projectDir, config, getLogger().isDebugEnabled() );

        // We will return a different project object using the new model (hence the need to return a project, not just modify the parameter)
        MavenProject project = new MavenProject( model, artifactFactory, mavenTools, repositoryHelper, this, config );

        Artifact projectArtifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                                        project.getVersion(), project.getPackaging() );
        project.setArtifact( projectArtifact );
        project.setParentFile( parentFile );

        validateModel( model, pomFile );
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
            superProject = new MavenProject( model, artifactFactory, mavenTools, repositoryHelper, this, config );
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

        MavenProject mavenProject;
        try
        {
            mavenProject = projectBuilder.buildFromLocalPath( new FileInputStream( projectDescriptor ), Arrays.asList(
                getSuperProject( config, projectDescriptor, true ).getModel() ), null, null, resolver,
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
            try
            {
                Writer out = WriterFactory.newXmlWriter( System.out );
                MavenXpp3Writer writer = new MavenXpp3Writer();
                writer.write( out, model );
                out.close();
            }
            catch ( IOException e )
            {

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
