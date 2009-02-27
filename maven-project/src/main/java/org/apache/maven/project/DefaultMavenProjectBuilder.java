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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.builder.*;
import org.apache.maven.project.builder.profile.ProfileContext;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.repository.MavenRepositorySystem;
import org.apache.maven.repository.VersionNotFoundException;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


/**
 * @version $Id$
 */
@Component(role = MavenProjectBuilder.class)
public class DefaultMavenProjectBuilder
    implements MavenProjectBuilder, LogEnabled
{
    @Requirement
    private ModelValidator validator;

    @Requirement
    private ProfileAdvisor profileAdvisor;

    @Requirement
    private MavenRepositorySystem repositorySystem;

    @Requirement
    List<ModelEventListener> listeners;
    
    private Logger logger;
    
    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

    // the aspect weaving seems not to work for reflection from plugin.

    private Map processedProjectCache = new HashMap();

    private static HashMap<String, MavenProject> hm = new HashMap<String, MavenProject>();    
    
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
        if ( projectDescriptor == null )
        {
            throw new IllegalArgumentException( "projectDescriptor: null" );
        }

        if ( config == null )
        {
            throw new IllegalArgumentException( "config: null" );
        }

        List<ArtifactRepository> artifactRepositories = new ArrayList<ArtifactRepository>();
        try
        {
            artifactRepositories.addAll( repositorySystem.buildArtifactRepositories( getSuperModel().getRepositories() ) );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( "Cannot create repositories from super model.", e.getMessage() );
        }
        
        if ( config.getRemoteRepositories() != null )
        {
            artifactRepositories.addAll( config.getRemoteRepositories() );
        }

        MavenProject project = readModelFromLocalPath( "unknown", projectDescriptor, new DefaultPomArtifactResolver( config.getLocalRepository(), artifactRepositories, repositorySystem ), config );

        project.setFile( projectDescriptor );

        project = buildWithProfiles( project.getModel(), config, projectDescriptor, project.getParentFile());

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

    //!! This is used by the RR plugin
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
        
        List<ArtifactRepository> artifactRepositories = new ArrayList<ArtifactRepository>( remoteArtifactRepositories );
        try
        {
            artifactRepositories.addAll( repositorySystem.buildArtifactRepositories( getSuperModel().getRepositories() ) );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( "Cannot create repositories from super model.", e.getMessage() );
        }
        
        File f = (artifact.getFile() != null) ? artifact.getFile() : new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
        try
        {
            repositorySystem.findModelFromRepository( artifact, artifactRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( project.getId(), "Error resolving project artifact.", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( project.getId(), "Error finding project artifact.", e );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( project.getId(), "Error with repository specified in project.", e );
        }

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );

        project = readModelFromLocalPath( "unknown", artifact.getFile(), new DefaultPomArtifactResolver( config.getLocalRepository(), artifactRepositories, repositorySystem ), config );
        project = buildWithProfiles( project.getModel(), config, artifact.getFile(), project.getParentFile() );
        artifact.setFile( f );
        project.setVersion( artifact.getVersion() );

        hm.put( artifact.getId(), project );
        
        return project;
    }

    /**
     * This is used for pom-less execution like running archetype:generate.
     * 
     * I am taking out the profile handling and the interpolation of the base directory until we spec
     * this out properly.
     */
    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();
                       
        MavenProject project = null;
        
        try
        {
            project = new MavenProject( superModel, repositorySystem, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            // Not going to happen.
        }

        try
        {
            project.setRemoteArtifactRepositories( repositorySystem.buildArtifactRepositories( superModel.getRepositories() ) );
            project.setPluginArtifactRepositories( repositorySystem.buildArtifactRepositories( superModel.getRepositories() ) );
        }
        catch ( InvalidRepositoryException e )
        {
            // Not going to happen.
        }

        project.setExecutionRoot( true );

        return project;
    }

    public MavenProjectBuildingResult buildProjectWithDependencies( File projectDescriptor, ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        MavenProject project = build( projectDescriptor, config );

        try
        {
            project.setDependencyArtifacts( repositorySystem.createArtifacts( project.getDependencies(), null, null, project ) );
        }
        catch ( VersionNotFoundException e )
        {
            InvalidDependencyVersionException ee = new InvalidDependencyVersionException( e.getProjectId(), e.getDependency(), e.getPomFile(), e.getCauseException() );
            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ),
                                                "Unable to build project due to an invalid dependency version: " +
                                                    e.getMessage(), projectDescriptor, ee );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( project.getArtifact() )
            .setArtifactDependencies( project.getDependencyArtifacts() )
            .setLocalRepository( config.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() )
            .setMetadataSource( repositorySystem );

        ArtifactResolutionResult result = repositorySystem.resolve( request );
                
        project.setArtifacts( result.getArtifacts() );

        return new MavenProjectBuildingResult( project, result );
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    private MavenProject buildWithProfiles( Model model, ProjectBuilderConfiguration config, File projectDescriptor,
                                        File parentDescriptor )
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
            profileActivationContext = new ProfileActivationContext( config.getExecutionProperties(), false );
        }

        List<Profile> projectProfiles = new ArrayList<Profile>();

        projectProfiles.addAll( profileAdvisor.applyActivatedProfiles( model, profileActivationContext ) );

        projectProfiles.addAll( profileAdvisor.applyActivatedExternalProfiles( model, externalProfileManager ) );

        MavenProject project;
        
        try
        {
            project = new MavenProject( model, repositorySystem, this, config );
            
            validateModel( model, projectDescriptor );

            Artifact projectArtifact = repositorySystem.createBuildArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), project.getPackaging() );
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

    private MavenProject readModelFromLocalPath( String projectId, File projectDescriptor, PomArtifactResolver resolver, ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        if ( projectDescriptor == null )
        {
            throw new IllegalArgumentException( "projectDescriptor: null, Project Id =" + projectId );
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        
        interpolatorProperties.addAll( InterpolatorProperty.toInterpolatorProperties( config.getExecutionProperties(), 
                PomInterpolatorTag.EXECUTION_PROPERTIES.name()));
        
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
            mavenProject = buildFromLocalPath( projectDescriptor, interpolatorProperties, resolver, config, this );
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
        if ( parent != null && parent.getFile() != null && parent.getModel().getBuild() != null)
        {
            parent.getModel().getBuild().setDirectory( parent.getFile().getAbsolutePath() );
            setBuildOutputDirectoryOnParent( parent );
        }
    }

    public PomClassicDomainModel buildModel( File pom,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             PomArtifactResolver resolver )
        throws IOException
    {
        return buildModel( pom, interpolatorProperties, null, null, resolver );
    }

    private PomClassicDomainModel buildModel(File pom,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             Collection<String> activeProfileIds, Collection<String> inactiveProfileIds,
                                             PomArtifactResolver resolver)
        throws IOException
    {
        if ( pom == null )
        {
            throw new IllegalArgumentException( "pom: null" );
        }

        if ( resolver == null )
        {
            throw new IllegalArgumentException( "resolver: null" );
        }

        if(activeProfileIds == null)
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
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        domainModels.add( domainModel );

        //Process Profile on most specialized child model
        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(domainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, inactiveProfileIds, properties);

        Collection<ModelContainer> profileContainers = profileContext.getActiveProfiles();

        for(ModelContainer mc : profileContainers)
        {
            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            //transformed.add(new ModelProperty(ProjectUri.xUri, null));
            for(ModelProperty mp : mc.getProperties())
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                        && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri) )
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }
            domainModels.add(new PomClassicDomainModel(transformed));
        }

        File parentFile = null;
        int lineageCount = 0;
        if ( domainModel.getParentId() != null )
        {
            List<DomainModel> mavenParents;
            if ( isParentLocal( domainModel.getRelativePathOfParent(), pom.getParentFile() ) )
            {
                mavenParents =
                    getDomainModelParentsFromLocalPath( domainModel, resolver, pom.getParentFile(), properties,
                                                        activeProfileIds, inactiveProfileIds );
            }
            else
            {
                mavenParents =
                    getDomainModelParentsFromRepository( domainModel, resolver, properties, activeProfileIds,
                                                         inactiveProfileIds );
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

        domainModels.add( convertToDomainModel( getSuperModel() ) );

        PomTransformer transformer = new PomTransformer( new PomClassicDomainModelFactory() );

        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );

        PomClassicDomainModel transformedDomainModel = ( (PomClassicDomainModel) ctx.transform( domainModels,
                                                                                                transformer,
                                                                                                transformer,
                                                                                                Collections.EMPTY_LIST,
                                                                                                properties,
                                                                                                listeners ) );
        // Lineage count is inclusive to add the POM read in itself.
        transformedDomainModel.setLineageCount( lineageCount + 1 );
        transformedDomainModel.setParentFile( parentFile );

        return transformedDomainModel;
    }

    private PomClassicDomainModel convertToDomainModel(Model model) throws IOException
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
        return new PomClassicDomainModel(new ByteArrayInputStream(baos.toByteArray()));
    }

    public MavenProject buildFromLocalPath(File pom,
                                           Collection<InterpolatorProperty> interpolatorProperties,
                                           PomArtifactResolver resolver,
                                           ProjectBuilderConfiguration projectBuilderConfiguration,
                                           MavenProjectBuilder mavenProjectBuilder)
        throws IOException
    {

       List<String> activeProfileIds = (projectBuilderConfiguration != null &&
                projectBuilderConfiguration.getGlobalProfileManager() != null &&
                projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext() != null) ?
               projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyActiveProfileIds() : new ArrayList<String>();

       List<String> inactiveProfileIds =
           ( projectBuilderConfiguration != null && projectBuilderConfiguration.getGlobalProfileManager() != null &&
                           projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext() != null ) ?
                           projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyInactiveProfileIds() : new ArrayList<String>();

        PomClassicDomainModel domainModel = buildModel( pom,
                interpolatorProperties,
                                                        activeProfileIds, inactiveProfileIds,
                                                        resolver );

        try
        {
            MavenProject mavenProject = new MavenProject( convertFromInputStreamToModel(domainModel.getInputStream()),
                                                          repositorySystem,
                                                          mavenProjectBuilder,
                                                          projectBuilderConfiguration );

            mavenProject.setParentFile( domainModel.getParentFile() );

            return mavenProject;
        }
        catch ( InvalidRepositoryException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    private static Model convertFromInputStreamToModel(InputStream inputStream) throws IOException
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
     * Returns true if the relative path of the specified parent references a pom, otherwise returns false.
     *
     * @param relativePath         the parent model info
     * @param projectDirectory the project directory of the child pom
     * @return true if the relative path of the specified parent references a pom, otherwise returns fals
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

    private List<DomainModel> getDomainModelParentsFromRepository( PomClassicDomainModel domainModel,
                                                                   PomArtifactResolver artifactResolver,
                                                                   List<InterpolatorProperty> properties,
                                                                   Collection<String> activeProfileIds,
                                                                   Collection<String> inactiveProfileIds )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        String parentId = domainModel.getParentId();

        if ( parentId == null )
        {
            return domainModels;
        }

        Artifact artifactParent = repositorySystem.createParentArtifact( domainModel.getParentGroupId(),
                domainModel.getParentArtifactId(), domainModel.getParentVersion() );

        artifactResolver.resolve( artifactParent );

        PomClassicDomainModel parentDomainModel = new PomClassicDomainModel( artifactParent.getFile() );

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            logger.debug( "Parent pom ids do not match: Parent File = " + artifactParent.getFile().getAbsolutePath() +
                ": Child ID = " + domainModel.getId() );
            return domainModels;
        }

        domainModels.add( parentDomainModel );

        //Process Profiles
        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(parentDomainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, inactiveProfileIds, properties);
        Collection<ModelContainer> profileContainers = profileContext.getActiveProfiles();

        for(ModelContainer mc : profileContainers)
        {
            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            transformed.add(new ModelProperty(ProjectUri.xUri, null));
            for(ModelProperty mp : mc.getProperties())
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                        && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri) )
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }

            domainModels.add(new PomClassicDomainModel(transformed));
        }

        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver, properties,
                                                                  activeProfileIds, inactiveProfileIds ) );
        return domainModels;
    }

    /**
     * Returns list of domain model parents of the specified domain model. The parent domain models are part
     *
     * @param domainModel
     * @param artifactResolver
     * @param projectDirectory
     * @return
     * @throws IOException
     */
    private List<DomainModel> getDomainModelParentsFromLocalPath( PomClassicDomainModel domainModel,
                                                                  PomArtifactResolver artifactResolver,
                                                                  File projectDirectory,
                                                                  List<InterpolatorProperty> properties,
                                                                  Collection<String> activeProfileIds,
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

        //Process Profiles
        ProfileContext profileContext = new ProfileContext(new DefaultModelDataSource(parentDomainModel.getModelProperties(),
                PomTransformer.MODEL_CONTAINER_FACTORIES), activeProfileIds, inactiveProfileIds, properties);
        Collection<ModelContainer> profileContainers = profileContext.getActiveProfiles();

        for(ModelContainer mc : profileContainers)
        {
            List<ModelProperty> transformed = new ArrayList<ModelProperty>();
            transformed.add(new ModelProperty(ProjectUri.xUri, null));
            for(ModelProperty mp : mc.getProperties())
            {
                if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.xUri) && !mp.getUri().equals(ProjectUri.Profiles.Profile.id)
                    && !mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.xUri))
                {
                    transformed.add(new ModelProperty(mp.getUri().replace(ProjectUri.Profiles.Profile.xUri, ProjectUri.xUri),
                            mp.getResolvedValue()));
                }
            }
            domainModels.add(new PomClassicDomainModel(transformed));
        }

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            logger.info( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = "
                    + parentDomainModel.getId() + ", Child ID = " + domainModel.getId() + ", Expected Parent ID = "
                    + domainModel.getParentId() );

            List<DomainModel> parentDomainModels =
                getDomainModelParentsFromRepository( domainModel, artifactResolver, properties, activeProfileIds,
                                                     inactiveProfileIds );

            if(parentDomainModels.size() == 0)
            {
                throw new IOException("Unable to find parent pom on local path or repo: "
                        + domainModel.getParentId());
            }

            domainModels.addAll( parentDomainModels );
            return domainModels;
        }

        domainModels.add( parentDomainModel );
        if ( domainModel.getParentId() != null )
        {
            if ( isParentLocal(parentDomainModel.getRelativePathOfParent(), parentFile.getParentFile() ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( parentDomainModel, artifactResolver,
                                                                         parentFile.getParentFile(), properties,
                                                                         activeProfileIds, inactiveProfileIds ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, artifactResolver,
                                                                          properties, activeProfileIds,
                                                                          inactiveProfileIds ) );
            }
        }

        return domainModels;
    }

    private DomainModel superDomainModel;

    // Super Model Handling

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    private MavenXpp3Reader modelReader = new MavenXpp3Reader();

    private Model superModel;

    public Model getSuperModel()
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