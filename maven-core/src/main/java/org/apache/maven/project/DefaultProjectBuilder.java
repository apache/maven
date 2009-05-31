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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolator.Interpolator;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.management.ManagementInjector;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.profile.ProfileActivationException;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.validation.ModelValidationResult;
import org.apache.maven.model.validation.ModelValidator;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @version $Id$
 */
@Component(role = ProjectBuilder.class)
public class DefaultProjectBuilder
    implements ProjectBuilder
{
    @Requirement
    private Logger logger;

    @Requirement
    private ModelReader modelReader;

    @Requirement
    private ModelValidator validator;

    @Requirement
    private LifecycleExecutor lifecycle;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ModelNormalizer normalizer;

    @Requirement
    private InheritanceAssembler inheritanceAssembler;

    @Requirement
    private Interpolator interpolator;

    @Requirement
    private ProfileSelector profileSelector;

    @Requirement
    private ProfileInjector profileInjector;

    @Requirement
    private ManagementInjector managementInjector;

    @Requirement
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Requirement
    private PluginConfigurationExpander pluginConfigurationExpander;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;
    
    @Requirement
    private MavenProjectCache projectCache;
    
    private MavenProject superProject;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File pomFile, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        return build( pomFile, pomFile.getParentFile(), configuration );
    }

    private MavenProject build( File pomFile, File projectDirectory, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        String cacheKey = getCacheKey( pomFile, configuration );

        MavenProject project = projectCache.get( cacheKey );
                
        if ( project != null )
        {
            return project;
        }
        
        List<Model> models;
        
        try
        {
            models = build( "unknown", pomFile, configuration );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "", "", e );
        }

        Model model = models.get(0);
        
        model.setProjectDirectory( projectDirectory );

        //Profiles
        //
        // Active profiles can be contributed to the MavenExecutionRequest as well as from the POM

        List<Profile> projectProfiles;

        try
        {
            projectProfiles = new ArrayList<Profile>();
            projectProfiles.addAll( model.getProfiles() );
            if ( configuration.getProfiles() != null )
            {
                projectProfiles.addAll( configuration.getProfiles() );
            }
            projectProfiles = profileSelector.getActiveProfiles( projectProfiles, configuration );
        }
        catch ( ProfileActivationException e )
        {
            throw new ProjectBuildingException( model.getId(), "Failed to activate pom profiles.", e );
        }

        for ( Profile p : projectProfiles )
        {
            if ( !"pom".equals( p.getSource() ) )
            {
                logger.debug( "Merging profile into model (build): Model = " + model.getId() + ", Profile = "
                    + p.getId() );
                profileInjector.injectProfile( model, p );
            }
        }
        
        try
        {
            model = interpolateModel( model, configuration, pomFile );

            if ( configuration.isProcessPlugins() )
            {                
                lifecycleBindingsInjector.injectLifecycleBindings( model );
            }

            managementInjector.injectManagement( model );

            validateModel( model, pomFile, configuration.istLenientValidation() );

            File parentFile = ( models.size() > 1 ) ? models.get( 1 ).getPomFile() : null;

            project = fromModelToMavenProject( model, parentFile, configuration, pomFile );

            if ( configuration.isProcessPlugins() )
            {
                pluginConfigurationExpander.expandPluginConfiguration( project.getModel() );

                lifecycle.populateDefaultConfigurationForPlugins( project.getModel().getBuild().getPlugins(), configuration.getLocalRepository(), project.getRemoteArtifactRepositories() );
            }
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "", "", e );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new ProjectBuildingException( "", e.getMessage(), e );
        }

        Build build = project.getBuild();
        // NOTE: setting this script-source root before path translation, because
        // the plugin tools compose basedir and scriptSourceRoot into a single file.
        project.addScriptSourceRoot( build.getScriptSourceDirectory() );
        project.addCompileSourceRoot( build.getSourceDirectory() );
        project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
        project.setFile( pomFile );
        project.setActiveProfiles( projectProfiles );
                
        projectCache.put( cacheKey, project );
                
        return project;
    }

    private String getCacheKey( File pomFile, ProjectBuildingRequest configuration )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        buffer.append( pomFile.getAbsolutePath() );
        buffer.append( '/' ).append( pomFile.lastModified() );
        return buffer.toString();
    }

    public MavenProject build( Artifact artifact, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        if ( !artifact.getType().equals( "pom" ) )
        {
            artifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setLocalRepository( configuration.getLocalRepository() )
            .setRemoteRepostories( configuration.getRemoteRepositories() );
        
        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( artifact.getId(), "Error resolving project artifact.", e );
        }

        return build( artifact.getFile(), null, configuration );
    }

    /**
     * This is used for pom-less execution like running archetype:generate.
     * 
     * I am taking out the profile handling and the interpolation of the base directory until we
     * spec this out properly.
     */
    public MavenProject buildStandaloneSuperProject( ProjectBuildingRequest config )
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

    public MavenProjectBuildingResult buildProjectWithDependencies( File pomFile, ProjectBuildingRequest request )
        throws ProjectBuildingException
    {
        MavenProject project = build( pomFile, request );

        Artifact artifact = new ProjectArtifact( project );                     
        
        ArtifactResolutionRequest artifactRequest = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( false )
            .setResolveTransitively( true )
            .setLocalRepository( request.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() );

        ArtifactResolutionResult result = repositorySystem.resolve( artifactRequest );

        if ( result.hasExceptions() )
        {
            Exception e = result.getExceptions().get( 0 );

            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                pomFile, e );
        }

        project.setArtifacts( result.getArtifacts() );
        
        return new MavenProjectBuildingResult( project, result );
    }

    private Model interpolateModel( Model model, ProjectBuildingRequest config, File projectDescriptor )
        throws ProjectBuildingException
    {
        try
        {
            model = interpolator.interpolateModel( model, config.getExecutionProperties(), model.getProjectDirectory() );
        }
        catch ( IOException e )
        {
            String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );
            throw new ProjectBuildingException( projectId, "", projectDescriptor, e );
        }

        return model;
    }

    private MavenProject fromModelToMavenProject( Model model, File parentFile, ProjectBuildingRequest config, File projectDescriptor )
        throws InvalidProjectModelException, IOException
    {
        MavenProject project;
        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );
        try
        {
            project = new MavenProject( model, repositorySystem, this, config );

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

    private List<Model> build( String projectId, File pomFile, ProjectBuildingRequest projectBuilderConfiguration )
        throws ProjectBuildingException, IOException
    {
        Model mainModel = readModel( projectId, pomFile, !projectBuilderConfiguration.istLenientValidation() );
        mainModel.setProjectDirectory( pomFile.getParentFile() );

        List<Model> domainModels = new ArrayList<Model>();

        domainModels.add( mainModel );

        ArtifactRepository localRepository = projectBuilderConfiguration.getLocalRepository();

        List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();
        try
        {
            for ( Profile profile : profileSelector.getActiveProfiles( projectBuilderConfiguration.getProfiles(),
                                                                       projectBuilderConfiguration ) )
            {
                for ( Repository repository : profile.getRepositories() )
                {
                    try
                    {
                        remoteRepositories.add( repositorySystem.buildArtifactRepository( repository ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        throw new ProjectBuildingException( projectId, "Failed to create remote repository "
                            + repository, pomFile, e );
                    }
                }
            }
            remoteRepositories = repositorySystem.getMirrors( remoteRepositories );
        }
        catch ( ProfileActivationException e )
        {
            throw new ProjectBuildingException( projectId, "Failed to determine active profiles", pomFile, e );
        }
        remoteRepositories.addAll( projectBuilderConfiguration.getRemoteRepositories() );

        if ( mainModel.getParent() != null )
        {
            List<Model> mavenParents;
            
            if ( isParentLocal( mainModel.getParent().getRelativePath(), pomFile.getParentFile() ) )
            {
                mavenParents = getDomainModelParentsFromLocalPath( mainModel, localRepository, remoteRepositories, pomFile.getParentFile(), projectBuilderConfiguration );
            }
            else
            {
                mavenParents = getDomainModelParentsFromRepository( mainModel, localRepository, remoteRepositories );
            }

            domainModels.addAll( mavenParents );
        }

        for ( Model model : domainModels )
        {
            normalizer.mergeDuplicates( model );
        }

        domainModels.add( getSuperModel() );
        List<Model> profileModels = new ArrayList<Model>();
        //Process Profiles
        for ( Model model : domainModels )
        {
            if ( !model.getProfiles().isEmpty() )
            {
                Collection<Profile> profiles;
                try
                {
                    profiles =
                        profileSelector.getActiveProfiles( model.getProfiles(), projectBuilderConfiguration );
                }
                catch ( ProfileActivationException e )
                {
                    throw new ProjectBuildingException( projectId, "Failed to determine active profiles", pomFile, e );
                }
                if ( !profiles.isEmpty() )
                {
                    for ( Profile p : profiles )
                    {
                        logger.debug( "Merging profile into model: Model = " + model.getId() + ", Profile = " + p.getId() );
                        profileInjector.injectProfile( model, p );
                    }
                }
            }
            profileModels.add( model );
        }

        processModelsForInheritance( profileModels );

        return profileModels;
    }

    private Model processModelsForInheritance( List<Model> models )
    {
        List<Model> parentsFirst = new ArrayList<Model>( models );
        Collections.reverse( parentsFirst );

        Model previousModel = null;

        for ( Model currentModel : parentsFirst )
        {
            inheritanceAssembler.assembleModelInheritance( currentModel, previousModel );
            previousModel = currentModel;
        }

        return previousModel;
    }

    private void validateModel( Model model, File pomFile, boolean lenient )
        throws InvalidProjectModelException
    {
        // Must validate before artifact construction to make sure dependencies are good
        ModelValidationResult validationResult = validator.validateEffectiveModel( model, lenient );

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        if ( validationResult.getMessageCount() > 0 )
        {
            for ( String s : validationResult.getMessages() )
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
        File f = new File( projectDirectory, relativePath ).getAbsoluteFile();

        if ( f.isDirectory() )
        {
            f = new File( f, "pom.xml" );
        }

        return f.isFile();
    }

    private List<Model> getDomainModelParentsFromRepository( Model model, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws IOException, ProjectBuildingException
    {
        List<Model> models = new ArrayList<Model>();

        Parent parent = model.getParent();

        if ( parent == null || localRepository == null )
        {
            return models;
        }
        
        Artifact artifactParent =
            repositorySystem.createProjectArtifact( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifactParent )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( remoteRepositories );
        
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
        
        Model parentModel = readModel( parent.getId(), artifactParent.getFile(), true );

        if ( !isMatchingParent( parentModel, parent ) )
        {
            //shane: what does this mean exactly and why does it occur
            logger.debug( "Parent pom ids do not match: Parent File = " + artifactParent.getFile().getAbsolutePath() + ": Child ID = " + model.getId() );

            // return domainModels;
            // TODO: review the proper handling of this, can it happen at all and if should we really continue or error out?
        }

        models.add( parentModel );

        models.addAll( getDomainModelParentsFromRepository( parentModel, localRepository, remoteRepositories ) );
        return models;
    }

    /**
     * Returns list of domain model parents of the specified domain model. The parent domain models
     * are part
     * 
     * @param domainModel
     * @param projectDirectory
     * @return
     * @throws IOException
     * @throws ProjectBuildingException 
     */
    private List<Model> getDomainModelParentsFromLocalPath( Model model, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories, File projectDirectory,
                                                                  ProjectBuildingRequest projectBuilderConfiguration )
        throws IOException, ProjectBuildingException
    {
        List<Model> models = new ArrayList<Model>();

        Parent parent = model.getParent();

        if ( parent == null )
        {
            return models;
        }

        File parentFile = new File( projectDirectory, parent.getRelativePath() ).getCanonicalFile();
        if ( parentFile.isDirectory() )
        {
            parentFile = new File( parentFile.getAbsolutePath(), "pom.xml" );
        }
        
        if ( !parentFile.isFile() )
        {
            throw new IOException( "File does not exist: File = " + parentFile.getAbsolutePath() );
        }
        
        Model parentModel = readModel( parent.getId(), parentFile, true );
        parentModel.setProjectDirectory( parentFile.getParentFile() );

        if ( !isMatchingParent( parentModel, parent ) )
        {
            logger.info( "Parent pom ids do not match: Parent File = " + parentFile.getAbsolutePath() + ", Parent ID = " + parentModel.getId() + ", Child ID = " + model.getId()
                + ", Expected Parent ID = " + parent.getId() );

            List<Model> parentModels = getDomainModelParentsFromRepository( model, localRepository, remoteRepositories );

            if ( parentModels.isEmpty() )
            {
                throw new IOException( "Unable to find parent pom on local path or repo: " + parent.getId() );
            }

            models.addAll( parentModels );
            return models;
        }

        models.add( parentModel );

        if ( parentModel.getParent() != null )
        {
            if ( isParentLocal( parentModel.getParent().getRelativePath(), parentFile.getParentFile() ) )
            {
                models.addAll( getDomainModelParentsFromLocalPath( parentModel, localRepository, remoteRepositories,
                                                                   parentFile.getParentFile(),
                                                                   projectBuilderConfiguration ) );
            }
            else
            {
                models.addAll( getDomainModelParentsFromRepository( parentModel, localRepository, remoteRepositories ) );
            }
        }

        return models;
    }

    private boolean isMatchingParent( Model parentModel, Parent parent )
    {
        if ( parentModel.getGroupId() != null )
        {
            if ( !parent.getGroupId().equals( parentModel.getGroupId() ) )
            {
                return false;
            }
        }
        else if ( parentModel.getParent() == null || !parent.getGroupId().equals( parentModel.getParent().getGroupId() ) )
        {
            return false;
        }
        if ( !parent.getArtifactId().equals( parentModel.getArtifactId() ) )
        {
            return false;
        }
        if ( parentModel.getVersion() != null )
        {
            if ( !parent.getVersion().equals( parentModel.getVersion() ) )
            {
                return false;
            }
        }
        else if ( parentModel.getParent() == null || !parent.getVersion().equals( parentModel.getParent().getVersion() ) )
        {
            return false;
        }
        return true;
    }

    private Model readModel( String projectId, File pomFile, boolean strict )
        throws ProjectBuildingException
    {
        Model model;

        Map<String, Object> options =
            Collections.<String, Object> singletonMap( ModelReader.IS_STRICT, Boolean.valueOf( strict ) );
        try
        {
            model = modelReader.read( pomFile, options );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "Failed to read POM for " + projectId + " from " + pomFile
                + ": " + e.getMessage(), pomFile, e );
        }

        validator.validateRawModel( model, !strict );

        return model;
    }

    // Super Model Handling

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    private Model superModel;

    protected Model getSuperModel()
    {
        if ( superModel != null )
        {
            return superModel;
        }

        String superPomResource = "/org/apache/maven/project/pom-" + MAVEN_MODEL_VERSION + ".xml";

        try
        {
            superModel = modelReader.read( getClass().getResourceAsStream( superPomResource ), null );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "The super POM is damaged"
                + ", please verify the integrity of your Maven installation", e );
        }

        return superModel;
    }

}