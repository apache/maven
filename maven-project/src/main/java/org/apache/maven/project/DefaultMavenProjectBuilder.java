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
import org.apache.maven.artifact.ArtifactStatus;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.ManagedVersionMap;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.build.model.DefaultModelLineage;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.build.model.ModelLineageIterator;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.injection.ModelDefaultsInjector;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    implements MavenProjectBuilder,
    Initializable, LogEnabled
{
    protected MavenProfilesBuilder profilesBuilder;

    protected ArtifactResolver artifactResolver;

    protected ArtifactMetadataSource artifactMetadataSource;

    private ArtifactFactory artifactFactory;

    private ModelInheritanceAssembler modelInheritanceAssembler;

    private ModelValidator validator;

    // TODO: make it a component
    private MavenXpp3Reader modelReader;

    private PathTranslator pathTranslator;

    private ModelDefaultsInjector modelDefaultsInjector;

    private ModelInterpolator modelInterpolator;

    private ModelLineageBuilder modelLineageBuilder;

    private ProfileAdvisor profileAdvisor;

    private MavenTools mavenTools;

    private ProjectWorkspace projectWorkspace;

    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

// the aspect weaving seems not to work for reflection from plugin.
    private Map processedProjectCache = new HashMap();


    public static final String MAVEN_MODEL_VERSION = "4.0.0";

    public void initialize()
    {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File projectDescriptor,
                               ArtifactRepository localRepository,
                               ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository )
                                                                                     .setGlobalProfileManager( profileManager );

        return buildFromSourceFileInternal( projectDescriptor, config );
    }

    public MavenProject build( File projectDescriptor,
                               ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        return buildFromSourceFileInternal( projectDescriptor, config );
    }

    /** @deprecated  */
    public MavenProject buildFromRepository( Artifact artifact,
                                             List remoteArtifactRepositories,
                                             ArtifactRepository localRepository,
                                             boolean allowStub )
        throws ProjectBuildingException

    {
        return buildFromRepository( artifact, remoteArtifactRepositories, localRepository );
    }


    public MavenProject buildFromRepository( Artifact artifact,
                                             List remoteArtifactRepositories,
                                             ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        String artifactKey = artifact.getId();

        MavenProject project = null;
        if ( !Artifact.LATEST_VERSION.equals( artifact.getVersion() ) && !Artifact.RELEASE_VERSION.equals( artifact.getVersion() ) )
        {
//            getLogger().debug( "Checking cache for project (in buildFromRepository): " + artifactKey );
            project = projectWorkspace.getProject( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }

        if ( project == null )
        {
//            getLogger().debug( "Allowing buildFromRepository to proceed for: " + artifactKey );

            Model model = findModelFromRepository( artifact, remoteArtifactRepositories, localRepository );

            ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );

            project = buildInternal( model, config, remoteArtifactRepositories, artifact.getFile(),
                                  false, false, false );
        }
//        else
//        {
//            getLogger().debug( "Returning cached project: " + project );
//        }

        return project;
    }

    private Logger logger;

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
        return buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration().setGlobalProfileManager( profileManager ) );
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
            List activated = profileAdvisor.applyActivatedProfiles( superModel, null, false, profileManager.getProfileActivationContext() );
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

        MavenProject project = new MavenProject( superModel );

        String projectId = safeVersionlessKey( STANDALONE_SUPERPOM_GROUPID, STANDALONE_SUPERPOM_ARTIFACTID );

        project.setManagedVersionMap(
            createManagedVersionMap( projectId, superModel.getDependencyManagement(), null ) );

        getLogger().debug( "Activated the following profiles for standalone super-pom: " + activeProfiles );
        project.setActiveProfiles( activeProfiles );


        try
        {
            processProjectLogic( project, null, config, null, true, true );

            project.setRemoteArtifactRepositories( mavenTools.buildArtifactRepositories( superModel.getRepositories() ) );
            project.setPluginArtifactRepositories( mavenTools.buildArtifactRepositories( superModel.getRepositories() ) );
        }
        catch ( InvalidRepositoryException e )
        {
            // we shouldn't be swallowing exceptions, no matter how unlikely.
            // or, if we do, we should pay attention to the one coming from getSuperModel()...
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":"
                                                + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid repository!",
                                                e );
        }
        catch ( ModelInterpolationException e )
        {
            // we shouldn't be swallowing exceptions, no matter how unlikely.
            // or, if we do, we should pay attention to the one coming from getSuperModel()...
            throw new ProjectBuildingException( STANDALONE_SUPERPOM_GROUPID + ":"
                                                + STANDALONE_SUPERPOM_ARTIFACTID,
                                                "Maven super-POM contains an invalid expressions!",
                                                e );
        }

        project.setOriginalModel( superModel );

        project.setExecutionRoot( true );

        return project;
    }

    /** @since 2.0.x */
    public MavenProject buildWithDependencies( File projectDescriptor,
                                               ArtifactRepository localRepository,
                                               ProfileManager profileManager )
        throws ProjectBuildingException
    {
        return buildProjectWithDependencies( projectDescriptor, localRepository, profileManager ).getProject();
    }

    /** @since 2.1 */
    public MavenProjectBuildingResult buildProjectWithDependencies( File projectDescriptor,
                                                             ArtifactRepository localRepository,
                                                             ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository )
                                                                                     .setGlobalProfileManager( profileManager );

        return buildProjectWithDependencies( projectDescriptor, config );
    }

    public MavenProjectBuildingResult buildProjectWithDependencies( File projectDescriptor,
                                                                    ProjectBuilderConfiguration config )
               throws ProjectBuildingException
   {
        MavenProject project = build( projectDescriptor, config );

        // ----------------------------------------------------------------------
        // Typically when the project builder is being used from maven proper
        // the transitive dependencies will not be resolved here because this
        // requires a lot of work when we may only be interested in running
        // something simple like 'm2 clean'. So the artifact collector is used
        // in the dependency resolution phase if it is required by any of the
        // goals being executed. But when used as a component in another piece
        // of code people may just want to build maven projects and have the
        // dependencies resolved for whatever reason: this is why we keep
        // this snippet of code here.
        // ----------------------------------------------------------------------

        Artifact projectArtifact = project.getArtifact();

        String projectId = safeVersionlessKey( project.getGroupId(), project.getArtifactId() );

        Map managedVersions = project.getManagedVersionMap();

        try
        {
            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ProjectBuildingException( projectId,
                "Unable to build project due to an invalid dependency version: " +
                    e.getMessage(), projectDescriptor, e );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( projectArtifact )
            .setArtifactDependencies( project.getDependencyArtifacts() )
            .setLocalRepository( config.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( managedVersions )
            .setMetadataSource( artifactMetadataSource );

        ArtifactResolutionResult result = artifactResolver.resolve( request );

        project.setArtifacts( result.getArtifacts() );

        return new MavenProjectBuildingResult( project, result );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Map createManagedVersionMap( String projectId,
                                         DependencyManagement dependencyManagement, File pomFile )
        throws ProjectBuildingException
    {
        Map map = null;
        List deps;
        if ( ( dependencyManagement != null ) && ( ( deps = dependencyManagement.getDependencies() ) != null ) && ( deps.size() > 0 ) )
        {
            map = new ManagedVersionMap( map );

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Adding managed dependencies for " + projectId );
            }

            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );

                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(),
                        d.getClassifier(), d.getScope(), d.isOptional() );

                    if ( Artifact.SCOPE_SYSTEM.equals( d.getScope() ) && ( d.getSystemPath() != null ) )
                    {
                        artifact.setFile( new File( d.getSystemPath() ) );
                    }

                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "  " + artifact );
                    }

                    // If the dependencyManagement section listed exclusions,
                    // add them to the managed artifacts here so that transitive
                    // dependencies will be excluded if necessary.

                    if ( ( null != d.getExclusions() ) && !d.getExclusions().isEmpty() )
                    {
                        List exclusions = new ArrayList();

                        for ( Iterator j = d.getExclusions().iterator(); j.hasNext(); )
                        {
                            Exclusion e = (Exclusion) j.next();

                            exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                        }

                        ExcludesArtifactFilter eaf = new ExcludesArtifactFilter( exclusions );

                        artifact.setDependencyFilter( eaf );
                    }
                    else
                    {
                        artifact.setDependencyFilter( null );
                    }

                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new InvalidDependencyVersionException( projectId, d, pomFile, e );
                }
            }
        }
        else if ( map == null )
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    private MavenProject buildFromSourceFileInternal( File projectDescriptor,
                                                      ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
//        getLogger().debug( "Checking cache-hit on project (in build*): " + projectDescriptor );

        MavenProject project = projectWorkspace.getProject( projectDescriptor );

        if ( project == null )
        {
//            getLogger().debug( "Allowing project-build to proceed for: " + projectDescriptor );

            Model model = readModel( "unknown", projectDescriptor, STRICT_MODEL_PARSING );

            project = buildInternal( model,
                config,
                buildArtifactRepositories( getSuperModel() ),
                projectDescriptor,
                STRICT_MODEL_PARSING,
                true,
                true );
        }
//        else
//        {
//            getLogger().debug( "Returning cached project: " + project );
//        }

        return project;
    }

    private Model findModelFromRepository( Artifact artifact,
                                           List remoteArtifactRepositories,
                                           ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        String projectId = safeVersionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        remoteArtifactRepositories = normalizeToArtifactRepositories( remoteArtifactRepositories, projectId );

        Artifact projectArtifact;

        // if the artifact is not a POM, we need to construct a POM artifact based on the artifact parameter given.
        if ( "pom".equals( artifact.getType() ) )
        {
            projectArtifact = artifact;
        }
        else
        {
            getLogger().warn( "Attempting to build MavenProject instance for Artifact (" + artifact.getGroupId() + ":"
                + artifact.getArtifactId() + ":" + artifact.getVersion() + ") of type: "
                + artifact.getType() + "; constructing POM artifact instead." );

            projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getScope() );
        }

        Model model;

        try
        {
            artifactResolver.resolve( projectArtifact, remoteArtifactRepositories, localRepository );

            File file = projectArtifact.getFile();

            model = readModel( projectId, file, STRICT_MODEL_PARSING );

            String downloadUrl = null;

            ArtifactStatus status = ArtifactStatus.NONE;

            DistributionManagement distributionManagement = model.getDistributionManagement();

            if ( distributionManagement != null )
            {
                downloadUrl = distributionManagement.getDownloadUrl();

                status = ArtifactStatus.valueOf( distributionManagement.getStatus() );
            }

            checkStatusAndUpdate( projectArtifact, status, file, remoteArtifactRepositories, localRepository );

            // TODO: this is gross. Would like to give it the whole model, but maven-artifact shouldn't depend on that
            // Can a maven-core implementation of the Artifact interface store it, and be used in the exceptions?
            if ( downloadUrl != null )
            {
                projectArtifact.setDownloadUrl( downloadUrl );
            }
            else
            {
                projectArtifact.setDownloadUrl( model.getUrl() );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( projectId, "Error getting POM for '" + projectId + "' from the repository: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( projectId, "POM '" + projectId + "' not found in repository: " + e.getMessage(), e );
        }

        return model;
    }

    private List normalizeToArtifactRepositories( List remoteArtifactRepositories,
                                                  String projectId )
        throws ProjectBuildingException
    {
        List normalized = new ArrayList( remoteArtifactRepositories.size() );

        boolean normalizationNeeded = false;
        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            Object item = it.next();

            if ( item instanceof ArtifactRepository )
            {
                normalized.add( item );
            }
            else if ( item instanceof Repository )
            {
                Repository repo = (Repository) item;
                try
                {
                    item = mavenTools.buildArtifactRepository( repo );

                    normalized.add( item );
                    normalizationNeeded = true;
                }
                catch ( InvalidRepositoryException e )
                {
                    throw new ProjectBuildingException( projectId, "Error building artifact repository for id: " + repo.getId(), e );
                }
            }
            else
            {
                throw new ProjectBuildingException( projectId, "Error building artifact repository from non-repository information item: " + item );
            }
        }

        if ( normalizationNeeded )
        {
            return normalized;
        }
        else
        {
            return remoteArtifactRepositories;
        }
    }

    private void checkStatusAndUpdate( Artifact projectArtifact,
                                       ArtifactStatus status,
                                       File file,
                                       List remoteArtifactRepositories,
                                       ArtifactRepository localRepository )
        throws ArtifactNotFoundException
    {
        // TODO: configurable actions dependant on status
        if ( !projectArtifact.isSnapshot() && ( status.compareTo( ArtifactStatus.DEPLOYED ) < 0 ) )
        {
            // use default policy (enabled, daily update, warn on bad checksum)
            ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy();
            // TODO: re-enable [MNG-798/865]
            policy.setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );

            if ( policy.checkOutOfDate( new Date( file.lastModified() ) ) )
            {
                getLogger().info(
                    projectArtifact.getArtifactId() + ": updating metadata due to status of '" + status + "'" );
                try
                {
                    projectArtifact.setResolved( false );
                    artifactResolver.resolveAlways( projectArtifact, remoteArtifactRepositories, localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    getLogger().warn( "Error updating POM - using existing version" );
                    getLogger().debug( "Cause", e );
                }
                catch ( ArtifactNotFoundException e )
                {
                    getLogger().warn( "Error updating POM - not found. Removing local copy." );
                    getLogger().debug( "Cause", e );
                    file.delete();
                    throw e;
                }
            }
        }
    }

    // jvz:note
    // We've got a mixture of things going in the USD and from the repository, sometimes the descriptor
    // is a real file and sometimes null which makes things confusing.
    private MavenProject buildInternal( Model model,
                                        ProjectBuilderConfiguration config,
                                        List parentSearchRepositories,
                                        File projectDescriptor,
                                        boolean strict,
                                        boolean isReactorProject,
                                        boolean fromSourceTree )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        MavenProject superProject = new MavenProject( superModel );

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        // FIXME: Find a way to pass in this context, so it's never null!
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
                throw new ProjectBuildingException( projectId, "Failed to activate external profiles.", projectDescriptor, e );
            }

            profileActivationContext = externalProfileManager.getProfileActivationContext();
        }
        else
        {
            profileActivationContext = new DefaultProfileActivationContext( config.getExecutionProperties(), false );
        }

        LinkedHashSet activeInSuperPom = new LinkedHashSet();
        List activated = profileAdvisor.applyActivatedProfiles( superModel, projectDescriptor, isReactorProject, profileActivationContext );
        if ( !activated.isEmpty() )
        {
            activeInSuperPom.addAll( activated );
        }

        activated = profileAdvisor.applyActivatedExternalProfiles( superModel, projectDescriptor, externalProfileManager );
        if ( !activated.isEmpty() )
        {
            activeInSuperPom.addAll( activated );
        }

        superProject.setActiveProfiles( activated );

        //noinspection CollectionDeclaredAsConcreteClass
        LinkedList lineage = new LinkedList();

        LinkedHashSet aggregatedRemoteWagonRepositories = collectInitialRepositories( model, superModel,
            parentSearchRepositories,
            projectDescriptor,
            isReactorProject,
            profileActivationContext );

        Model originalModel = ModelUtils.cloneModel( model );

        MavenProject project;

        try
        {
            project = assembleLineage( model, lineage, config, projectDescriptor, aggregatedRemoteWagonRepositories, strict, isReactorProject );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( projectId, e.getMessage(), e );
        }

        project.setOriginalModel( originalModel );

        // we don't have to force the collision exception for superModel here, it's already been done in getSuperModel()
        MavenProject previousProject = superProject;

        Model previous = superProject.getModel();

        for ( Iterator i = lineage.iterator(); i.hasNext(); )
        {
            MavenProject currentProject = (MavenProject) i.next();

            Model current = currentProject.getModel();

            String pathAdjustment = null;

            try
            {
                pathAdjustment = previousProject.getModulePathAdjustment( currentProject );
            }
            catch ( IOException e )
            {
                getLogger().debug(
                    "Cannot determine whether " + currentProject.getId() + " is a module of " + previousProject.getId() + ". Reason: " + e.getMessage(),
                    e );
            }

            modelInheritanceAssembler.assembleModelInheritance( current, previous, pathAdjustment );

            previous = current;
            previousProject = currentProject;
        }

        // only add the super repository if it wasn't overridden by a profile or project
        List repositories = new ArrayList( aggregatedRemoteWagonRepositories );

        List superRepositories = buildArtifactRepositories( superModel );

        for ( Iterator i = superRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        // merge any duplicated plugin definitions together, using the first appearance as the dominant one.
        ModelUtils.mergeDuplicatePluginDefinitions( project.getModel().getBuild() );

        try
        {
            project = processProjectLogic( project, projectDescriptor, config, repositories, strict, false );
        }
        catch ( ModelInterpolationException e )
        {
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
        }

        if ( fromSourceTree )
        {
            Build build = project.getBuild();

            // NOTE: setting this script-source root before path translation, because
            // the plugin tools compose basedir and scriptSourceRoot into a single file.
            project.addScriptSourceRoot( build.getScriptSourceDirectory() );

            getLogger().debug( "Aligning project: " + project.getId() + " to base directory: " + projectDescriptor.getParentFile() );
            pathTranslator.alignToBaseDirectory( project.getModel(), projectDescriptor.getParentFile() );

            project.addCompileSourceRoot( build.getSourceDirectory() );

            project.addTestCompileSourceRoot( build.getTestSourceDirectory() );

            // Only track the file of a POM in the source tree
            project.setFile( projectDescriptor );
        }

//        getLogger().debug( "Caching project: " + project.getId() + " (also keyed by file: " + project.getFile() + ")" );

        projectWorkspace.storeProjectByCoordinate( project );
        projectWorkspace.storeProjectByFile( project );

        project.setManagedVersionMap( createManagedVersionMap( projectId, project.getDependencyManagement(), projectDescriptor ) );

        return project;
    }

    /*
     * Order is:
     *
     * 1. model profile repositories
     * 2. model repositories
     * 3. superModel profile repositories
     * 4. superModel repositories
     * 5. parentSearchRepositories
     */
    private LinkedHashSet collectInitialRepositories( Model model,
                                                      Model superModel,
                                                      List parentSearchRepositories,
                                                      File pomFile,
                                                      boolean validProfilesXmlLocation,
                                                      ProfileActivationContext profileActivationContext )
        throws ProjectBuildingException
    {
        LinkedHashSet collected = new LinkedHashSet();

        collectInitialRepositoriesFromModel( collected, model, pomFile, validProfilesXmlLocation, profileActivationContext );

        collectInitialRepositoriesFromModel( collected, superModel, null, validProfilesXmlLocation, profileActivationContext );

        if ( ( parentSearchRepositories != null ) && !parentSearchRepositories.isEmpty() )
        {
            collected.addAll( parentSearchRepositories );
        }

        return collected;
    }

    private void collectInitialRepositoriesFromModel( LinkedHashSet collected,
                                                      Model model,
                                                      File pomFile,
                                                      boolean validProfilesXmlLocation,
                                                      ProfileActivationContext profileActivationContext )
        throws ProjectBuildingException
    {
        Set reposFromProfiles = profileAdvisor.getArtifactRepositoriesFromActiveProfiles( model, pomFile, validProfilesXmlLocation, profileActivationContext );

        if ( ( reposFromProfiles != null ) && !reposFromProfiles.isEmpty() )
        {
            collected.addAll( reposFromProfiles );
        }

        List modelRepos = model.getRepositories();

        if ( ( modelRepos != null ) && !modelRepos.isEmpty() )
        {
            try
            {
                collected.addAll( mavenTools.buildArtifactRepositories( modelRepos ) );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new ProjectBuildingException( safeVersionlessKey( model.getGroupId(), model.getArtifactId() ),
                    "Failed to construct ArtifactRepository instances for repositories declared in: "
                        + model.getId(), e );
            }
        }
    }

    private String safeVersionlessKey( String groupId,
                                       String artifactId )
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

    private List buildArtifactRepositories( Model model )
        throws ProjectBuildingException
    {
        try
        {
            return mavenTools.buildArtifactRepositories( model.getRepositories() );
        }
        catch ( InvalidRepositoryException e )
        {
            String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

            throw new ProjectBuildingException( projectId, e.getMessage(), e );
        }
    }

    /**
     * @todo can this take in a model instead of a project and still be successful?
     * @todo In fact, does project REALLY need a MavenProject as a parent? Couldn't it have just a wrapper around a
     * model that supported parents which were also the wrapper so that inheritence was assembled. We don't really need
     * the resolved source roots, etc for the parent - that occurs for the parent when it is constructed independently
     * and projects are not cached or reused
     */
    private MavenProject processProjectLogic( MavenProject project,
                                              File pomFile,
                                              ProjectBuilderConfiguration config,
                                              List remoteRepositories,
                                              boolean strict,
                                              boolean isSuperPom )
        throws ProjectBuildingException, ModelInterpolationException, InvalidRepositoryException
    {
        Model model = project.getModel();

        List activeProfiles = project.getActiveProfiles();

        // TODO: Clean this up...we're using this to 'jump' the interpolation step for model properties not expressed in XML.
        //  [BP] - Can this above comment be explained?
        // We don't need all the project methods that are added over those in the model, but we do need basedir
        // mkleint - using System.getProperties() is almost definitely bad for embedding.
        Map context = new HashMap();

        // [MNG-2339] ensure the system properties are still interpolated for backwards compat, but the model values must win
        if ( config.getExecutionProperties() != null && !config.getExecutionProperties().isEmpty() )
        {
            context.putAll( config.getExecutionProperties() );
        }

        File projectDir = null;

        if ( pomFile != null )
        {
            projectDir = pomFile.getAbsoluteFile().getParentFile();
        }

        Map overrideContext = new HashMap();
        if ( !isSuperPom && config.getUserProperties() != null && !config.getUserProperties().isEmpty() )
        {
            overrideContext.putAll( config.getUserProperties() );
        }

        model = modelInterpolator.interpolate( model, context, overrideContext, projectDir, true );

        // We must inject any imported dependencyManagement information ahead of the defaults injection.
        if ( !isSuperPom )
        {
            // TODO: [jdcasey] This line appears to be part of the problem for MNG-3391...
            // the same line is in 2.0.x, so this is related to caching changes too...need to figure out how the two interact.
            mergeManagedDependencies( model, config.getLocalRepository(), remoteRepositories );
        }

        // interpolation is before injection, because interpolation is off-limits in the injected variables
        modelDefaultsInjector.injectDefaults( model );

        MavenProject parentProject = project.getParent();

        Model originalModel = project.getOriginalModel();

        Artifact parentArtifact = project.getParentArtifact();

        // We will return a different project object using the new model (hence the need to return a project, not just modify the parameter)
        project = new MavenProject( model );

        project.setOriginalModel( originalModel );

        project.setActiveProfiles( activeProfiles );

        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        Artifact projectArtifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
            project.getVersion(), project.getPackaging() );
        project.setArtifact( projectArtifact );

//        project.setPluginArtifactRepositories( mavenTools.buildArtifactRepositories( model.getPluginRepositories() ) );

        DistributionManagement dm = model.getDistributionManagement();

        if ( dm != null )
        {
            ArtifactRepository repo = mavenTools.buildDeploymentArtifactRepository( dm.getRepository() );
            project.setReleaseArtifactRepository( repo );

            if ( dm.getSnapshotRepository() != null )
            {
                repo = mavenTools.buildDeploymentArtifactRepository( dm.getSnapshotRepository() );
                project.setSnapshotArtifactRepository( repo );
            }
        }

        project.setParent( parentProject );

        if ( parentProject != null )
        {
            project.setParentArtifact( parentArtifact );
        }

        validateModel( model, pomFile );

        try
        {
            LinkedHashSet repoSet = new LinkedHashSet();
            if ( ( model.getRepositories() != null ) && !model.getRepositories().isEmpty() )
            {
                repoSet.addAll( model.getRepositories() );
            }

            if ( ( model.getPluginRepositories() != null ) && !model.getPluginRepositories().isEmpty() )
            {
                repoSet.addAll( model.getPluginRepositories() );
            }

            project.setRemoteArtifactRepositories(
                                                  mavenTools.buildArtifactRepositories( new ArrayList( repoSet ) ) );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        // TODO: these aren't taking active project artifacts into consideration in the reactor
        project.setPluginArtifacts( createPluginArtifacts( projectId, project.getBuildPlugins(), pomFile ) );

        project.setReportArtifacts( createReportArtifacts( projectId, project.getReportPlugins(), pomFile ) );

        project.setExtensionArtifacts( createExtensionArtifacts( projectId, project.getBuildExtensions(), pomFile ) );

        return project;
    }

    private void validateModel( Model model,
                                File pomFile )
        throws InvalidProjectModelException
    {
        // Must validate before artifact construction to make sure dependencies are good
        ModelValidationResult validationResult = validator.validate( model );

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new InvalidProjectModelException( projectId, "Failed to validate POM", pomFile,
                validationResult );
        }
    }

    /**
     * @param isReactorProject
     * @noinspection CollectionDeclaredAsConcreteClass
     * @todo We need to find an effective way to unit test parts of this method!
     * @todo Refactor this into smaller methods with discrete purposes.
     */
    private MavenProject assembleLineage( Model model,
                                          LinkedList lineage,
                                          ProjectBuilderConfiguration config,
                                          File pomFile,
                                          Set aggregatedRemoteWagonRepositories,
                                          boolean strict,
                                          boolean isReactorProject )
        throws ProjectBuildingException, InvalidRepositoryException
    {
        ModelLineage modelLineage = new DefaultModelLineage();

        modelLineage.setOrigin( model, pomFile, new ArrayList( aggregatedRemoteWagonRepositories ), isReactorProject );

        modelLineageBuilder.resumeBuildingModelLineage( modelLineage, config, !strict, isReactorProject );

        // FIXME: Find a way to pass in this context, so it's never null!
        ProfileActivationContext profileActivationContext;
        ProfileManager externalProfileManager = config.getGlobalProfileManager();

        if ( externalProfileManager != null )
        {
            profileActivationContext = externalProfileManager.getProfileActivationContext();
        }
        else
        {
            profileActivationContext = new DefaultProfileActivationContext( config.getExecutionProperties(), false );
        }

        MavenProject lastProject = null;
        for ( ModelLineageIterator it = modelLineage.lineageIterator(); it.hasNext(); )
        {
            Model currentModel = (Model) it.next();

            File currentPom = it.getPOMFile();

            MavenProject project = new MavenProject( currentModel );
            project.setFile( currentPom );

            if ( lastProject != null )
            {
                // TODO: Use cached parent project here, and stop looping, if possible...
                lastProject.setParent( project );
                project = lastProject.getParent();

                lastProject.setParentArtifact( artifactFactory.createParentArtifact( project.getGroupId(), project
                    .getArtifactId(), project.getVersion() ) );
            }

            // NOTE: the caching aspect may replace the parent project instance, so we apply profiles here.
            // TODO: Review this...is that a good idea, to allow application of profiles when other profiles could have been applied already?
            project.setActiveProfiles( profileAdvisor.applyActivatedProfiles( project.getModel(), project.getFile(), isReactorProject, profileActivationContext ) );

            lineage.addFirst( project );

            lastProject = project;
        }

        MavenProject result = (MavenProject) lineage.getLast();

        if ( externalProfileManager != null )
        {
            LinkedHashSet active = new LinkedHashSet();

            List existingActiveProfiles = result.getActiveProfiles();
            if ( ( existingActiveProfiles != null ) && !existingActiveProfiles.isEmpty() )
            {
                active.addAll( existingActiveProfiles );
            }

            profileAdvisor.applyActivatedExternalProfiles( result.getModel(), pomFile, externalProfileManager );
        }

        return result;
    }

    private void mergeManagedDependencies(Model model, ArtifactRepository localRepository, List parentSearchRepositories)
        throws ProjectBuildingException
    {
        DependencyManagement modelDepMgmt = model.getDependencyManagement();

        if (modelDepMgmt != null)
        {
            Map depsMap = new TreeMap();
            Iterator iter = modelDepMgmt.getDependencies().iterator();
            boolean doInclude = false;
            while (iter.hasNext())
            {
                Dependency dep = (Dependency) iter.next();
                depsMap.put( dep.getManagementKey(), dep );

                // MNG-3391: SEE BELOW.
                if (dep.getType().equals("pom") && Artifact.SCOPE_IMPORT.equals( dep.getScope() ) )
                {
                    doInclude = true;
                }
            }
            Map newDeps = new TreeMap(depsMap);
            iter = modelDepMgmt.getDependencies().iterator();
            if (doInclude)
            {
                while (iter.hasNext())
                {
                    Dependency dep = (Dependency)iter.next();

                    // MNG-3391: The check for scope == 'import' to limit the StackOverflowExceptions caused
                    // when importing from the parent and the import-target is a module that declares the
                    // current pom as a parent.
                    //
                    // Also, dependencies with type == 'pom' are the best way we currently have to
                    // aggregate multiple other dependencies without messing with the issues caused by using
                    // an assembly (ClassCastException if a second-level dep is also part of the maven core,
                    // for instance)
                    if (dep.getType().equals("pom") && Artifact.SCOPE_IMPORT.equals( dep.getScope() ) )
                    {
                        Artifact artifact = artifactFactory.createProjectArtifact( dep.getGroupId(), dep.getArtifactId(),
                                                                                  dep.getVersion(), dep.getScope() );
                        MavenProject project = buildFromRepository(artifact, parentSearchRepositories, localRepository, false);

                        DependencyManagement depMgmt = project.getDependencyManagement();

                        if (depMgmt != null)
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug( "Importing managed dependencies for " + dep.toString() );
                            }

                            for ( Iterator it = depMgmt.getDependencies().iterator(); it.hasNext(); )
                            {
                                Dependency includedDep = (Dependency) it.next();
                                String key = includedDep.getManagementKey();
                                if (!newDeps.containsKey(key))
                                {
                                    newDeps.put( includedDep.getManagementKey(), includedDep );
                                }
                            }
                            newDeps.remove(dep.getManagementKey());
                        }
                    }
                }
                List deps = new ArrayList(newDeps.values());
                modelDepMgmt.setDependencies(deps);
            }
        }
    }

    private Model readModel( String projectId,
                             File file,
                             boolean strict )
        throws ProjectBuildingException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( file );

            String modelSource = IOUtil.toString( reader );

            checkModelVersion( modelSource, projectId, file );

            StringReader sReader = new StringReader( modelSource );

            try
            {
                return modelReader.read( sReader, strict );
            }
            catch ( XmlPullParserException e )
            {
                throw new InvalidProjectModelException( projectId, "Parse error reading POM. Reason: " + e.getMessage(),
                                                        file, e );
            }
        }
        catch ( FileNotFoundException e )
        {
            throw new ProjectBuildingException( projectId,
                "Could not find the model file '" + file.getAbsolutePath() + "'.", file, e );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "Failed to build model from file '" +
                file.getAbsolutePath() + "'.\nError: \'" + e.getLocalizedMessage() + "\'", file, e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private void checkModelVersion( String modelSource,
                                    String projectId,
                                    File file )
        throws InvalidProjectModelException
    {
        if ( modelSource.indexOf( "<modelVersion>" + MAVEN_MODEL_VERSION ) < 0 )
        {
            throw new InvalidProjectModelException( projectId, "Not a v" + MAVEN_MODEL_VERSION + " POM.", file );
        }
    }

    /**
     * @deprecated use {@link #createPluginArtifacts(String, List, File)}
     * @param projectId
     * @param plugins
     * @param pomLocation absolute path of pom file
     * @return
     * @throws ProjectBuildingException
     */
    protected Set createPluginArtifacts( String projectId,
                                         List plugins, String pomLocation )
        throws ProjectBuildingException
    {
        return createPluginArtifacts( projectId, plugins, new File( pomLocation ) );
    }

    /**
     *
     * @param projectId
     * @param plugins
     * @param pomLocation pom file
     * @return
     * @throws ProjectBuildingException
     */
    protected Set createPluginArtifacts( String projectId,
                                         List plugins, File pomLocation )
        throws ProjectBuildingException
    {
        Set pluginArtifacts = new HashSet();

        for ( Iterator i = plugins.iterator(); i.hasNext(); )
        {
            Plugin p = (Plugin) i.next();

            String version;
            if ( StringUtils.isEmpty( p.getVersion() ) )
            {
                version = "RELEASE";
            }
            else
            {
                version = p.getVersion();
            }

            Artifact artifact;
            try
            {
                artifact = artifactFactory.createPluginArtifact( p.getGroupId(), p.getArtifactId(),
                    VersionRange.createFromVersionSpec( version ) );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new InvalidProjectVersionException( projectId, "Plugin: " + p.getKey(), version, pomLocation, e );
            }

            if ( artifact != null )
            {
                pluginArtifacts.add( artifact );
            }
        }

        return pluginArtifacts;
    }

    /**
     * @deprecated use {@link #createReportArtifacts(String, List, File)}
     * @param projectId
     * @param reports
     * @param pomLocation absolute path of pom file
     * @return
     * @throws ProjectBuildingException
     */
    protected Set createReportArtifacts( String projectId,
                                         List reports, String pomLocation )
        throws ProjectBuildingException
    {
        return createReportArtifacts( projectId, reports, new File( pomLocation ) );
    }

    // TODO: share with createPluginArtifacts?
    protected Set createReportArtifacts( String projectId,
                                         List reports, File pomLocation )
        throws ProjectBuildingException
    {
        Set pluginArtifacts = new HashSet();

        if ( reports != null )
        {
            for ( Iterator i = reports.iterator(); i.hasNext(); )
            {
                ReportPlugin p = (ReportPlugin) i.next();

                String version;
                if ( StringUtils.isEmpty( p.getVersion() ) )
                {
                    version = "RELEASE";
                }
                else
                {
                    version = p.getVersion();
                }

                Artifact artifact;
                try
                {
                    artifact = artifactFactory.createPluginArtifact( p.getGroupId(), p.getArtifactId(),
                        VersionRange.createFromVersionSpec( version ) );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new InvalidProjectVersionException( projectId, "Report plugin: " + p.getKey(), version, pomLocation, e );
                }

                if ( artifact != null )
                {
                    pluginArtifacts.add( artifact );
                }
            }
        }

        return pluginArtifacts;
    }

    /**
     * @deprecated use {@link #createExtensionArtifacts(String, List, File)}
     * @param projectId
     * @param extensions
     * @param pomLocation absolute path of pom file
     * @return
     * @throws ProjectBuildingException
     */
    protected Set createExtensionArtifacts( String projectId,
                                            List extensions, String pomLocation )
        throws ProjectBuildingException
    {
        return createExtensionArtifacts( projectId, extensions, new File( pomLocation ) );
    }

    // TODO: share with createPluginArtifacts?
    protected Set createExtensionArtifacts( String projectId,
                                            List extensions, File pomFile )
        throws ProjectBuildingException
    {
        Set extensionArtifacts = new HashSet();

        if ( extensions != null )
        {
            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension ext = (Extension) i.next();

                String version;
                if ( StringUtils.isEmpty( ext.getVersion() ) )
                {
                    version = "RELEASE";
                }
                else
                {
                    version = ext.getVersion();
                }

                Artifact artifact;
                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( version );
                    artifact =
                        artifactFactory.createExtensionArtifact( ext.getGroupId(), ext.getArtifactId(), versionRange );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    String key = ArtifactUtils.versionlessKey( ext.getGroupId(), ext.getArtifactId() );
                    throw new InvalidProjectVersionException( projectId, "Extension: " + key,
                                                              version, pomFile, e );
                }

                if ( artifact != null )
                {
                    extensionArtifacts.add( artifact );
                }
            }
        }

        return extensionArtifacts;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

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

            checkModelVersion( modelSource, projectId, null );

            StringReader sReader = new StringReader( modelSource );

            return modelReader.read( sReader, STRICT_MODEL_PARSING );
        }
        catch ( XmlPullParserException e )
        {
            throw new InvalidProjectModelException( projectId, "Parse error reading POM. Reason: " + e.getMessage(), e );
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

    protected Logger getLogger()
    {
        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
}
