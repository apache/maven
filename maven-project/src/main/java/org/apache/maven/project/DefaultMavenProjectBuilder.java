package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileActivationContext;
import org.apache.maven.profiles.ProfileActivationException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.profiles.ProfileContext;
import org.apache.maven.project.processor.ProcessorContext;
import org.apache.maven.project.processor.ProfilesProcessor;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.VersionNotFoundException;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelEventListener;
import org.apache.maven.shared.model.ModelProperty;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @version $Id$
 */
@Component(role = MavenProjectBuilder.class)
public class DefaultMavenProjectBuilder
    implements MavenProjectBuilder
{
    @Requirement
    private Logger logger;
    
    @Requirement
    private ModelValidator validator;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private PlexusContainer container;

    @Requirement
    List<ModelEventListener> listeners;
    
    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;    

    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

    // the aspect weaving seems not to work for reflection from plugin.

    private Map processedProjectCache = new HashMap();

    private static HashMap<String, MavenProject> hm = new HashMap<String, MavenProject>();

    private MavenProject superProject;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    // This is used by the SITE plugin.
    public MavenProject build( File project, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()        
            .setLocalRepository( localRepository )
            .setGlobalProfileManager( profileManager );
        
        return build( project, configuration );
    }

    public MavenProject build( File pomFile, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        MavenProject project = readModelFromLocalPath( "unknown", pomFile, configuration.getLocalRepository(), configuration.getRemoteRepositories(), configuration );

        project.setFile( pomFile );
        project = buildWithProfiles( project.getModel(), configuration, pomFile, project.getParentFile() );

        Build build = project.getBuild();
        // NOTE: setting this script-source root before path translation, because
        // the plugin tools compose basedir and scriptSourceRoot into a single file.
        project.addScriptSourceRoot( build.getScriptSourceDirectory() );
        project.addCompileSourceRoot( build.getSourceDirectory() );
        project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
        project.setFile( pomFile );
        
        setBuildOutputDirectoryOnParent( project );

        hm.put( ArtifactUtils.artifactId( project.getGroupId(), project.getArtifactId(), "pom", project.getVersion() ), project );
   
        return project;
    }

    //!! This is used by the RR plugin
    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteArtifactRepositories, ArtifactRepository localRepository, boolean allowStubs )
        throws ProjectBuildingException
    {
        Artifact pomArtifact = artifact;
        
        if ( !artifact.getType().equals( "pom" ) )
        {
            pomArtifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }
        
        return buildFromRepository( pomArtifact, remoteArtifactRepositories, localRepository );
    }

    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        MavenProject project = hm.get( artifact.getId() );

        if ( project != null )
        {
            return project;
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( artifact, localRepository, remoteRepositories );
        ArtifactResolutionResult result = repositorySystem.resolve( request );
                
        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( artifact.getId(), "Error resolving project artifact.", e );
        }
        //Won't know anything about settings profiles in this path
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration()   
            .setLocalRepository( localRepository )
            .setRemoteRepositories( remoteRepositories );

        project = readModelFromLocalPath( "unknown", artifact.getFile(), config.getLocalRepository(), remoteRepositories, config );
        project = buildWithProfiles( project.getModel(), config, artifact.getFile(), project.getParentFile() );
        artifact.setFile( artifact.getFile() );
        project.setVersion( artifact.getVersion() );

        hm.put( artifact.getId(), project );

        return project;
    }

    /**
     * This is used for pom-less execution like running archetype:generate.
     * 
     * I am taking out the profile handling and the interpolation of the base directory until we
     * spec this out properly.
     */
    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        if ( superProject != null )
        {
            return superProject;
        }

        Model superModel = getSuperModel();

        try
        {
            superProject = new MavenProject( superModel, repositorySystem, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            // Not going to happen.
        }

        superProject.setExecutionRoot( true );

        return superProject;
    }

    public MavenProjectBuildingResult buildProjectWithDependencies( File pomFile, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        MavenProject project = build( pomFile, configuration );

        try
        {
            project.setDependencyArtifacts( repositorySystem.createArtifacts( project.getDependencies(), null, null, project ) );
        }
        catch ( VersionNotFoundException e )
        {
            InvalidDependencyVersionException ee = new InvalidDependencyVersionException( e.getProjectId(), e.getDependency(), e.getPomFile(), e.getCauseException() );
            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                pomFile, ee );
        }

        Artifact pomArtifact = repositorySystem.createProjectArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion() );
        pomArtifact.setFile( pomFile );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact( pomArtifact ).setArtifactDependencies( project.getDependencyArtifacts() )
            .setLocalRepository( configuration.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() );
        
        if(request.getRemoteRepostories() == null)
        {
            request.setRemoteRepostories( new ArrayList<ArtifactRepository>() );
        }

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        if ( result.hasExceptions() )
        {
            Exception e = result.getExceptions().get( 0 );

            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                pomFile, e );
        }

        project.setArtifacts( result.getArtifacts() );

        return new MavenProjectBuildingResult( project, result );
    }

    private MavenProject buildWithProfiles( Model model, ProjectBuilderConfiguration config, File projectDescriptor, File parentDescriptor )
        throws ProjectBuildingException
    {
        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        List<Profile> projectProfiles = new ArrayList<Profile>();
        ProfileManager externalProfileManager = config.getGlobalProfileManager();
        
        ProfileActivationContext profileActivationContext = (externalProfileManager == null) ? new ProfileActivationContext( config.getExecutionProperties(), false ):
            externalProfileManager.getProfileActivationContext();
     
        if(externalProfileManager != null)
        {           
            try
            {
                projectProfiles.addAll( externalProfileManager.getActiveProfiles( model ) );
            }
            catch ( ProfileActivationException e )
            {
                throw new ProjectBuildingException( projectId, "Failed to activate external profiles.", projectDescriptor,
                                                    e );
            }         
        }

        ProfileManager profileManager = new DefaultProfileManager( container, profileActivationContext );
        profileManager.addProfiles( model.getProfiles() );
        //System.out.println("PROFILE POM: COUNT = " + model.getProfiles().size());
        try
        {
            //System.out.println("PROFILE POM - ACTIVE: COUNT = " + profileManager.getActiveProfiles( model ).size());
            projectProfiles.addAll( profileManager.getActiveProfiles( model ) );
        }
        catch ( ProfileActivationException e )
        {
            throw new ProjectBuildingException( projectId, "Failed to activate pom profiles.", projectDescriptor,
                                                e );
        }

        if(!projectProfiles.isEmpty())
        {         
            try
            {
                PomClassicDomainModel dm = ProcessorContext.mergeProfilesIntoModel( projectProfiles, model, false );
                ProcessorContext.interpolateModelProperties( dm.getModelProperties(),
                                                             new ArrayList<InterpolatorProperty>(), dm );
                dm = new PomClassicDomainModel( dm.getModelProperties(), false );
                model = dm.getModel();
            }
            catch ( IOException e )
            {

                throw new ProjectBuildingException(projectId, "", projectDescriptor, e);
            }            
        }

        MavenProject project;

        try
        {
            project = new MavenProject( model, repositorySystem, this, config );

            validateModel( model, projectDescriptor );

            Artifact projectArtifact = repositorySystem.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging() );
            project.setArtifact( projectArtifact );

            project.setParentFile( parentDescriptor );

        }
        catch ( InvalidRepositoryException e )
        {
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
        }

        project.setActiveProfiles( projectProfiles );

        return project;
    }

    private MavenProject readModelFromLocalPath( String projectId, File pomFile, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories, ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();

        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getExecutionProperties(), PomInterpolatorTag.EXECUTION_PROPERTIES.name() ) );

        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getUserProperties(), PomInterpolatorTag.USER_PROPERTIES.name() ) );

        if ( config.getBuildStartTime() != null )
        {
            interpolatorProperties.add( new InterpolatorProperty( "${build.timestamp}", new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( config.getBuildStartTime() ),
                                                                  PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
        }

        MavenProject mavenProject;

        try
        {
            mavenProject = buildFromLocalPath( pomFile, interpolatorProperties, localRepository, remoteRepositories, config, this );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "File = " + pomFile.getAbsolutePath(), e );
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
                logger.debug( s );
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
        if ( parent != null && parent.getFile() != null && parent.getModel().getBuild() != null )
        {
            parent.getModel().getBuild().setDirectory( parent.getFile().getAbsolutePath() );
            setBuildOutputDirectoryOnParent( parent );
        }
    }

    protected PomClassicDomainModel buildModel( File pom, Collection<InterpolatorProperty> interpolatorProperties, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws IOException
    {
        return buildModel( pom, interpolatorProperties, null, null, localRepository, remoteRepositories );
    }

    private PomClassicDomainModel buildModel( File pom, Collection<InterpolatorProperty> interpolatorProperties, Collection<String> activeProfileIds, Collection<String> inactiveProfileIds,
                                              ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws IOException
    {
        if ( pom == null )
        {
            throw new IllegalArgumentException( "pom: null" );
        }

        if ( activeProfileIds == null )
        {
            activeProfileIds = new ArrayList<String>();
        }
        if ( inactiveProfileIds == null )
        {
            inactiveProfileIds = new ArrayList<String>();
        }

        List<InterpolatorProperty> properties;
        if ( interpolatorProperties == null )
        {
            properties = new ArrayList<InterpolatorProperty>();
        }
        else
        {
            properties = new ArrayList<InterpolatorProperty>( interpolatorProperties );
        }

        PomClassicDomainModel domainModel = new PomClassicDomainModel( pom );
        domainModel.setProjectDirectory( pom.getParentFile() );
        domainModel.setMostSpecialized( true );

        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        domainModels.add( domainModel );

        File parentFile = null;
        int lineageCount = 0;
        if ( domainModel.getParentId() != null )
        {
            List<DomainModel> mavenParents;
            if ( isParentLocal( domainModel.getRelativePathOfParent(), pom.getParentFile() ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( domainModel, localRepository, remoteRepositories, pom.getParentFile(), properties, activeProfileIds, inactiveProfileIds );
            }
            else
            {
                mavenParents = getDomainModelParentsFromRepository( domainModel, localRepository, remoteRepositories, properties, activeProfileIds, inactiveProfileIds );
            }

            if ( mavenParents.size() > 0 )
            {
                PomClassicDomainModel dm = (PomClassicDomainModel) mavenParents.get( 0 );
                parentFile = dm.getFile();
                domainModel.setParentFile( parentFile );
                lineageCount = mavenParents.size();
            }

            domainModels.addAll( mavenParents );
        }

        domainModels.add( convertToDomainModel( getSuperModel(), false ) );
        List<DomainModel> profileModels = new ArrayList<DomainModel>();
        //Process Profiles
        for(DomainModel domain : domainModels)
        {
            PomClassicDomainModel dm = (PomClassicDomainModel) domain;
            
            if(!dm.getModel().getProfiles().isEmpty())
            {
                 ProfileContext profileContext1 = new ProfileContext( dm.getModel().getProfiles(), activeProfileIds,
                                                                     inactiveProfileIds, properties );
                 Collection<Profile> profiles = profileContext1.getActiveProfiles();
                 if(!profiles.isEmpty())
                 {
                    profileModels.add(ProcessorContext.mergeProfilesIntoModel( profileContext1.getActiveProfiles(), dm.getModel(), 
                                                                                dm.isMostSpecialized() ));  
                 }
                 else
                 {
                     profileModels.add( dm );   
                 }
            }
            else
            {
                profileModels.add( dm );
            }       
        }

        PomClassicDomainModel transformedDomainModel = ProcessorContext.build( profileModels, properties );
        // Lineage count is inclusive to add the POM read in itself.
        transformedDomainModel.setLineageCount( lineageCount + 1 );
        transformedDomainModel.setParentFile( parentFile );

        return transformedDomainModel;
    }

    private PomClassicDomainModel convertToDomainModel( Model model, boolean isMostSpecialized )
        throws IOException
    {
        if ( model == null )
        {
            throw new IllegalArgumentException( "model: null" );
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            out = WriterFactory.newXmlWriter( baos );
            writer.write( out, model );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
        return new PomClassicDomainModel( new ByteArrayInputStream( baos.toByteArray() ), isMostSpecialized );
    }

    protected MavenProject buildFromLocalPath( File pom, Collection<InterpolatorProperty> interpolatorProperties, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                               ProjectBuilderConfiguration projectBuilderConfiguration, MavenProjectBuilder mavenProjectBuilder )
        throws IOException
    {

        List<String> activeProfileIds = ( projectBuilderConfiguration != null && projectBuilderConfiguration.getGlobalProfileManager() != null && projectBuilderConfiguration.getGlobalProfileManager()
            .getProfileActivationContext() != null ) ? projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyActiveProfileIds() : new ArrayList<String>();

        List<String> inactiveProfileIds = ( projectBuilderConfiguration != null && projectBuilderConfiguration.getGlobalProfileManager() != null && projectBuilderConfiguration
            .getGlobalProfileManager().getProfileActivationContext() != null ) ? projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyInactiveProfileIds()
                                                                              : new ArrayList<String>();

        PomClassicDomainModel domainModel = buildModel( pom, interpolatorProperties, activeProfileIds, inactiveProfileIds, localRepository, remoteRepositories );

        try
        {
            MavenProject mavenProject = new MavenProject( convertFromInputStreamToModel( domainModel.getInputStream() ), repositorySystem, mavenProjectBuilder, projectBuilderConfiguration );

            mavenProject.setParentFile( domainModel.getParentFile() );

            return mavenProject;
        }
        catch ( InvalidRepositoryException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    private static Model convertFromInputStreamToModel( InputStream inputStream )
        throws IOException
    {
        try
        {
            return new MavenXpp3Reader().read( ReaderFactory.newXmlReader( inputStream ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e.getMessage() );
        }

    }

    /**
     * Returns true if the relative path of the specified parent references a pom, otherwise returns
     * false.
     * 
     * @param relativePath the parent model info
     * @param projectDirectory the project directory of the child pom
     * @return true if the relative path of the specified parent references a pom, otherwise returns
     *         fals
     */
    private boolean isParentLocal( String relativePath, File projectDirectory )
    {
        try
        {
            File f = new File( projectDirectory, relativePath ).getCanonicalFile();

            if ( f.isDirectory() )
            {
                f = new File( f, "pom.xml" );
            }

            return f.isFile();
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    private List<DomainModel> getDomainModelParentsFromRepository(  PomClassicDomainModel domainModel, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                                                   List<InterpolatorProperty> properties, Collection<String> activeProfileIds, Collection<String> inactiveProfileIds )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        String parentId = domainModel.getParentId();

        if ( parentId == null || localRepository == null)
        {
            return domainModels;
        }

        Artifact artifactParent = repositorySystem.createProjectArtifact( domainModel.getParentGroupId(), domainModel.getParentArtifactId(), domainModel.getParentVersion() );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( artifactParent, localRepository, remoteRepositories );
        ArtifactResolutionResult result = repositorySystem.resolve( request );
        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw (IOException) new IOException( "The parent POM " + artifactParent
                + " could not be retrieved from any repository" ).initCause( e );
        }

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( artifactParent.getFile() );

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            //shane: what does this mean exactly and why does it occur
            logger.debug( "Parent pom ids do not match: Parent File = " + artifactParent.getFile().getAbsolutePath() + ": Child ID = " + domainModel.getId() );

           // return domainModels;
        }

        domainModels.add( parentDomainModel );

        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, localRepository, remoteRepositories, properties, activeProfileIds, inactiveProfileIds ) );
        return domainModels;
    }

    /**
     * Returns list of domain model parents of the specified domain model. The parent domain models
     * are part
     * 
     * @param domainModel
     * @param projectDirectory
     * @return
     * @throws IOException
     */
    private List<DomainModel> getDomainModelParentsFromLocalPath( PomClassicDomainModel domainModel, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                                                  File projectDirectory, List<InterpolatorProperty> properties, Collection<String> activeProfileIds,
                                                                  Collection<String> inactiveProfileIds )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        String parentId = domainModel.getParentId();

        if ( parentId == null )
        {
            return domainModels;
        }

        File parentFile = new File( projectDirectory, domainModel.getRelativePathOfParent() ).getCanonicalFile();
        if ( parentFile.isDirectory() )
        {
            parentFile = new File( parentFile.getAbsolutePath(), "pom.xml" );
        }

        if ( !parentFile.isFile() )
        {
            throw new IOException( "File does not exist: File = " + parentFile.getAbsolutePath() );
        }

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( parentFile );
        parentDomainModel.setProjectDirectory( parentFile.getParentFile() );

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            logger.info( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = " + parentDomainModel.getId() + ", Child ID = " + domainModel.getId()
                + ", Expected Parent ID = " + domainModel.getParentId() );

            List<DomainModel> parentDomainModels = getDomainModelParentsFromRepository( domainModel, localRepository, remoteRepositories, properties, activeProfileIds, inactiveProfileIds );

            if ( parentDomainModels.size() == 0 )
            {
                throw new IOException( "Unable to find parent pom on local path or repo: " + domainModel.getParentId() );
            }

            domainModels.addAll( parentDomainModels );
            return domainModels;
        }

        domainModels.add( parentDomainModel );
        if ( domainModel.getParentId() != null )
        {
            if ( isParentLocal( parentDomainModel.getRelativePathOfParent(), parentFile.getParentFile() ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( parentDomainModel, localRepository, remoteRepositories, parentFile.getParentFile(), properties, activeProfileIds,
                                                                         inactiveProfileIds ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, localRepository, remoteRepositories, properties, activeProfileIds, inactiveProfileIds ) );
            }
        }

        return domainModels;
    }

    private DomainModel superDomainModel;

    // Super Model Handling

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    private MavenXpp3Reader modelReader = new MavenXpp3Reader();

    private Model superModel;

    protected Model getSuperModel()
    {
        if ( superModel != null )
        {
            return superModel;
        }

        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( getClass().getClassLoader().getResource( "org/apache/maven/project/pom-" + MAVEN_MODEL_VERSION + ".xml" ) );

            superModel = modelReader.read( reader, true );
        }
        catch ( Exception e )
        {
            // Not going to happen we're reading the super pom embedded in the JAR
        }
        finally
        {
            IOUtil.close( reader );
        }

        return superModel;
    }

}