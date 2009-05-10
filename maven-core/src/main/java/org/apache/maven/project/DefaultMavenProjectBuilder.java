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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Build;
import org.apache.maven.model.DomainModel;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelEventListener;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ProcessorContext;
import org.apache.maven.model.Profile;
import org.apache.maven.model.interpolator.Interpolator;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.lifecycle.LifecycleBindingsInjector;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileActivationException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.ProfileManagerInfo;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
    private LifecycleExecutor lifecycle;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    List<ModelEventListener> listeners;

    @Requirement
    private Interpolator interpolator;

    @Requirement
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    //private static HashMap<String, MavenProject> hm = new HashMap<String, MavenProject>();

    private MavenProject superProject;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File pomFile, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        DomainModel domainModel;

        try
        {
            domainModel = build( "unknown", pomFile, configuration );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "", "", e );
        }

        //Profiles

        List<Profile> projectProfiles;
        Properties props = new Properties();
        props.putAll( configuration.getExecutionProperties() );

        try
        {
            projectProfiles = DefaultProfileManager.getActiveProfilesFrom( configuration.getGlobalProfileManager(), props, domainModel.getModel() );
        }
        catch ( ProfileActivationException e )
        {
            throw new ProjectBuildingException( "", "Failed to activate pom profiles." );
        }

        try
        {
            List<Profile> externalProfiles = new ArrayList<Profile>();
            for ( Profile p : projectProfiles )
            {
                if ( !"pom".equals( p.getSource() ) )
                {
                    logger.debug( "Merging profile into model (build): Model = " + domainModel.getId() + ", Profile = " + p.getId() );
                    externalProfiles.add( p );
                }
            }

            domainModel = ProcessorContext.mergeProfilesIntoModel( externalProfiles, domainModel );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "", "" );
        }

        //Interpolation & Management
        MavenProject project;
        try
        {
            Model model = interpolateDomainModel( domainModel, configuration, pomFile );

            lifecycleBindingsInjector.injectLifecycleBindings( model );

            ProcessorContext.processManagementNodes( model );

            project = this.fromDomainModelToMavenProject( model, domainModel.getParentFile(), configuration, pomFile );

            Collection<Plugin> pluginsFromProject = project.getModel().getBuild().getPlugins();

            // Merge the various sources for mojo configuration:
            // 1. default values from mojo descriptor
            // 2. POM values from per-plugin configuration
            // 3. POM values from per-execution configuration
            // These configuration sources are given in increasing order of dominance.

            // push plugin configuration down to executions
            for ( Plugin buildPlugin : pluginsFromProject )
            {
                Xpp3Dom dom = (Xpp3Dom) buildPlugin.getConfiguration();

                if ( dom != null )
                {
                    for ( PluginExecution e : buildPlugin.getExecutions() )
                    {
                        Xpp3Dom dom1 = Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) e.getConfiguration(), new Xpp3Dom( dom ) );
                        e.setConfiguration( dom1 );
                    }
                }
            }

            // merge in default values from mojo descriptor
            lifecycle.populateDefaultConfigurationForPlugins( pluginsFromProject, project, configuration.getLocalRepository() );

            project.getModel().getBuild().setPlugins( new ArrayList<Plugin>( pluginsFromProject ) );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "", "" );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new ProjectBuildingException( "", e.getMessage() );
        }

        //project.setActiveProfiles( projectProfiles );

        Build build = project.getBuild();
        // NOTE: setting this script-source root before path translation, because
        // the plugin tools compose basedir and scriptSourceRoot into a single file.
        project.addScriptSourceRoot( build.getScriptSourceDirectory() );
        project.addCompileSourceRoot( build.getSourceDirectory() );
        project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
        project.setFile( pomFile );
        project.setActiveProfiles( projectProfiles );

        setBuildOutputDirectoryOnParent( project );

        return project;
    }

    public MavenProject buildFromRepository( Artifact artifact, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        if ( !artifact.getType().equals( "pom" ) )
        {
            artifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( artifact, configuration.getLocalRepository(), configuration.getRemoteRepositories() );
        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( artifact.getId(), "Error resolving project artifact.", e );
        }

        return build( artifact.getFile(), configuration );
    }

    // This is used by the SITE plugin.
    public MavenProject build( File project, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository ).setGlobalProfileManager( profileManager );

        return build( project, configuration );
    }

    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        return buildFromRepository( artifact, new DefaultProjectBuilderConfiguration( localRepository, remoteRepositories ) );
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
        Artifact pomArtifact = repositorySystem.createProjectArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion() );
        pomArtifact.setFile( pomFile );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact( pomArtifact ).setResolveTransitively( true ).setArtifactDependencies( project.getDependencyArtifacts() )
            .setLocalRepository( configuration.getLocalRepository() ).setRemoteRepostories( project.getRemoteArtifactRepositories() ).setManagedVersionMap( project.getManagedVersionMap() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        if ( result.hasExceptions() )
        {
            Exception e = result.getExceptions().get( 0 );

            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                pomFile, e );
        }

        project.setArtifacts( result.getArtifacts() );
        project.getArtifacts().remove( pomArtifact );

        return new MavenProjectBuildingResult( project, result );
    }

    private Model interpolateDomainModel( DomainModel domainModel, ProjectBuilderConfiguration config, File projectDescriptor )
        throws ProjectBuildingException
    {
        Model model = domainModel.getModel();

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        try
        {
            model = interpolator.interpolateModel( model, config.getExecutionProperties(), domainModel.getProjectDirectory() );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "", projectDescriptor, e );
        }

        return model;
    }

    private MavenProject fromDomainModelToMavenProject( Model model, File parentFile, ProjectBuilderConfiguration config, File projectDescriptor )
        throws InvalidProjectModelException, IOException
    {
        MavenProject project;
        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );
        try
        {
            project = new MavenProject( model, repositorySystem, this, config );

            validateModel( model, projectDescriptor );

            Artifact projectArtifact = repositorySystem.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging() );
            project.setArtifact( projectArtifact );

            project.setParentFile( parentFile );

        }
        catch ( InvalidRepositoryException e )
        {
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
        }

        return project;
    }

    private DomainModel build( String projectId, File pomFile, ProjectBuilderConfiguration projectBuilderConfiguration )
        throws ProjectBuildingException, IOException
    {
        List<String> activeProfileIds = ( projectBuilderConfiguration != null && projectBuilderConfiguration.getGlobalProfileManager() != null && projectBuilderConfiguration.getGlobalProfileManager()
            .getProfileActivationContext() != null ) ? projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyActiveProfileIds() : new ArrayList<String>();

        List<String> inactiveProfileIds = ( projectBuilderConfiguration != null && projectBuilderConfiguration.getGlobalProfileManager() != null && projectBuilderConfiguration
            .getGlobalProfileManager().getProfileActivationContext() != null ) ? projectBuilderConfiguration.getGlobalProfileManager().getProfileActivationContext().getExplicitlyInactiveProfileIds()
                                                                              : new ArrayList<String>();

        ProfileManagerInfo profileInfo = new ProfileManagerInfo( projectBuilderConfiguration.getExecutionProperties(), activeProfileIds, inactiveProfileIds );
        DomainModel domainModel = new DomainModel( pomFile );
        domainModel.setProjectDirectory( pomFile.getParentFile() );
        domainModel.setMostSpecialized( true );

        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        domainModels.add( domainModel );
        ArtifactRepository localRepository = projectBuilderConfiguration.getLocalRepository();
        List<ArtifactRepository> remoteRepositories = projectBuilderConfiguration.getRemoteRepositories();

        File parentFile = null;
        int lineageCount = 0;
        if ( domainModel.getParentId() != null )
        {
            List<DomainModel> mavenParents;
            MavenProject topProject = projectBuilderConfiguration.getTopLevelProjectFromReactor();
            if ( useTopLevelProjectForParent( domainModel, topProject ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( domainModel, localRepository, remoteRepositories, topProject.getFile(), projectBuilderConfiguration );
            }
            else if ( isParentLocal( domainModel.getRelativePathOfParent(), pomFile.getParentFile() ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( domainModel, localRepository, remoteRepositories, pomFile.getParentFile(), projectBuilderConfiguration );
            }
            else
            {
                mavenParents = getDomainModelParentsFromRepository( domainModel, localRepository, remoteRepositories );
            }

            if ( mavenParents.size() > 0 )
            {
                DomainModel dm = mavenParents.get( 0 );
                parentFile = dm.getFile();
                domainModel.setParentFile( parentFile );
                lineageCount = mavenParents.size();
            }

            domainModels.addAll( mavenParents );
        }

        domainModels.add( new DomainModel( getSuperModel(), false ) );
        List<DomainModel> profileModels = new ArrayList<DomainModel>();
        //Process Profiles
        for ( DomainModel domain : domainModels )
        {
            DomainModel dm = domain;

            if ( !dm.getModel().getProfiles().isEmpty() )
            {
                Collection<Profile> profiles = DefaultProfileManager.getActiveProfiles( dm.getModel().getProfiles(), profileInfo );
                if ( !profiles.isEmpty() )
                {
                    for ( Profile p : profiles )
                    {
                        logger.debug( "Merging profile into model: Model = " + dm.getId() + ", Profile = " + p.getId() );
                    }
                    profileModels.add( ProcessorContext.mergeProfilesIntoModel( profiles, dm ) );
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

        DomainModel transformedDomainModel = ProcessorContext.build( profileModels, listeners );

        // Lineage count is inclusive to add the POM read in itself.
        transformedDomainModel.setLineageCount( lineageCount + 1 );
        transformedDomainModel.setParentFile( parentFile );

        return transformedDomainModel;
    }

    private static boolean useTopLevelProjectForParent( DomainModel currentModel, MavenProject topProject )
        throws IOException
    {
        if ( topProject == null || currentModel.getModel().getParent() == null )
        {
            return false;
        }

        return topProject.getGroupId().equals( currentModel.getParentGroupId() ) && topProject.getArtifactId().equals( currentModel.getParentArtifactId() )
            && topProject.getVersion().equals( currentModel.getParentVersion() );

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
                logger.error( s );
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

    /**
     * Returns true if the relative path of the specified parent references a pom, otherwise returns
     * false.
     * 
     * @param relativePath the parent model info
     * @param projectDirectory the project directory of the child pom
     * @return true if the relative path of the specified parent references a pom, otherwise returns
     *         fals
     */
    private static boolean isParentLocal( String relativePath, File projectDirectory )
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

    private List<DomainModel> getDomainModelParentsFromRepository( DomainModel domainModel, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();

        String parentId = domainModel.getParentId();

        if ( parentId == null || localRepository == null )
        {
            return domainModels;
        }

        Artifact artifactParent = repositorySystem.createProjectArtifact( domainModel.getParentGroupId(), domainModel.getParentArtifactId(), domainModel.getParentVersion() );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( artifactParent, localRepository, remoteRepositories );
        ArtifactResolutionResult result;
        try
        {
            result = repositorySystem.resolve( request );
        }
        catch ( Exception e )
        {
            throw (IOException) new IOException( "The parent POM " + artifactParent + " could not be retrieved from any repository" ).initCause( e );
        }

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw (IOException) new IOException( "The parent POM " + artifactParent + " could not be retrieved from any repository" ).initCause( e );
        }

        DomainModel parentDomainModel = new DomainModel( artifactParent.getFile() );

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            //shane: what does this mean exactly and why does it occur
            logger.debug( "Parent pom ids do not match: Parent File = " + artifactParent.getFile().getAbsolutePath() + ": Child ID = " + domainModel.getId() );

            // return domainModels;
        }

        domainModels.add( parentDomainModel );

        domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, localRepository, remoteRepositories ) );
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
    private List<DomainModel> getDomainModelParentsFromLocalPath( DomainModel domainModel, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories, File projectDirectory,
                                                                  ProjectBuilderConfiguration projectBuilderConfiguration )
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
        MavenProject topProject = projectBuilderConfiguration.getTopLevelProjectFromReactor();
        boolean isTop = useTopLevelProjectForParent( domainModel, topProject );
        DomainModel parentDomainModel = null;
        if ( !isTop )
        {
            if ( !parentFile.isFile() )
            {
                throw new IOException( "File does not exist: File = " + parentFile.getAbsolutePath() );
            }
            parentDomainModel = new DomainModel( parentFile );
            parentDomainModel.setProjectDirectory( parentFile.getParentFile() );
        }
        else
        {
            parentDomainModel = new DomainModel( projectBuilderConfiguration.getTopLevelProjectFromReactor().getFile() );
        }

        if ( !parentDomainModel.matchesParentOf( domainModel ) )
        {
            logger.info( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = " + parentDomainModel.getId() + ", Child ID = " + domainModel.getId()
                + ", Expected Parent ID = " + domainModel.getParentId() );

            List<DomainModel> parentDomainModels = getDomainModelParentsFromRepository( domainModel, localRepository, remoteRepositories );

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
            if ( isTop )
            {
                if ( isParentLocal( parentDomainModel.getRelativePathOfParent(), parentFile.getParentFile() ) )
                {
                    domainModels
                        .addAll( getDomainModelParentsFromLocalPath( parentDomainModel, localRepository, remoteRepositories, topProject.getFile().getParentFile(), projectBuilderConfiguration ) );
                }
                else
                {
                    domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, localRepository, remoteRepositories ) );
                }
            }
            else if ( isParentLocal( parentDomainModel.getRelativePathOfParent(), parentFile.getParentFile() ) )
            {
                domainModels.addAll( getDomainModelParentsFromLocalPath( parentDomainModel, localRepository, remoteRepositories, parentFile.getParentFile(), projectBuilderConfiguration ) );
            }
            else
            {
                domainModels.addAll( getDomainModelParentsFromRepository( parentDomainModel, localRepository, remoteRepositories ) );
            }
        }

        return domainModels;
    }

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