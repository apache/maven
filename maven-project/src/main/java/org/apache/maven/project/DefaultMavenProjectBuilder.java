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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactStatus;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
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
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.ProfilesConversionUtils;
import org.apache.maven.profiles.ProfilesRoot;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.project.injection.ProfileInjector;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
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
    extends AbstractLogEnabled
    implements MavenProjectBuilder, Initializable, Contextualizable
{
    // TODO: remove
    private PlexusContainer container;

    protected MavenProfilesBuilder profilesBuilder;

    protected ArtifactResolver artifactResolver;

    protected ArtifactMetadataSource artifactMetadataSource;

    private ArtifactFactory artifactFactory;

    private ModelInheritanceAssembler modelInheritanceAssembler;

    private ProfileInjector profileInjector;

    private ModelValidator validator;

    private Map rawProjectCache = new HashMap();

    private Map processedProjectCache = new HashMap();

    // TODO: make it a component
    private MavenXpp3Reader modelReader;

    private PathTranslator pathTranslator;

    private ModelDefaultsInjector modelDefaultsInjector;

    private ModelInterpolator modelInterpolator;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    // ----------------------------------------------------------------------
    // I am making this available for use with a new method that takes a
    // a monitor wagon monitor as a parameter so that tools can use the
    // methods here and receive callbacks. MNG-1015
    // ----------------------------------------------------------------------

    private WagonManager wagonManager;

    public static final String MAVEN_MODEL_VERSION = "4.0.0";

    public void initialize()
    {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File pom,
                               ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        return buildFromSourceFileInternal( pom, config, true );
    }

    public MavenProject build( File pom,
                               ProjectBuilderConfiguration config,
                               boolean checkDistributionManagementStatus )
        throws ProjectBuildingException
    {
        return buildFromSourceFileInternal( pom, config, checkDistributionManagementStatus );
    }

    public MavenProject build( File projectDescriptor,
                               ArtifactRepository localRepository,
                               ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository ).setGlobalProfileManager( profileManager );
        return buildFromSourceFileInternal( projectDescriptor, config, true );
    }

    public MavenProject build( File projectDescriptor,
                               ArtifactRepository localRepository,
                               ProfileManager profileManager,
                               boolean checkDistributionManagementStatus )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository ).setGlobalProfileManager( profileManager );
        return buildFromSourceFileInternal( projectDescriptor, config, checkDistributionManagementStatus );
    }

    // jvz:note
    // When asked for something from the repository are we getting it from the reactor? Yes, when using this call
    // we are assuming that the reactor has been run and we have collected the projects required to satisfy it0042
    // which means the projects in the reactor are required for finding classes in <project>/target/classes. Not
    // sure this is ideal. I remove all caching from the builder and all reactor related ITs which assume
    // access to simbling project resources failed.
    public MavenProject buildFromRepository( Artifact artifact,
                                             List remoteArtifactRepositories,
                                             ArtifactRepository localRepository,
                                             boolean allowStubModel )
        throws ProjectBuildingException
    {
        String cacheKey = createCacheKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );

        MavenProject project = (MavenProject) processedProjectCache.get( cacheKey );

        if ( project != null )
        {
            return project;
        }

        Model model = findModelFromRepository( artifact, remoteArtifactRepositories, localRepository, allowStubModel );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );

        return buildInternal( "Artifact [" + artifact + "]", model, config, remoteArtifactRepositories,
                              null, false );
    }

    public MavenProject buildFromRepository( Artifact artifact,
                                             List remoteArtifactRepositories,
                                             ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        return buildFromRepository( artifact, remoteArtifactRepositories, localRepository, true );
    }

    // what is using this externally? jvz.
    public MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        //TODO mkleint - use the (Container, Properties) constructor to make system properties embeddable
        ProfileManager profileManager = new DefaultProfileManager( container );

        return buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository )
                                                                                    .setGlobalProfileManager( profileManager ) );
    }

    public MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository,
                                                     ProfileManager profileManager )
        throws ProjectBuildingException
    {
        return buildStandaloneSuperProject( new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository )
                                                                                    .setGlobalProfileManager( profileManager ) );
    }

    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        superModel.setGroupId( STANDALONE_SUPERPOM_GROUPID );

        superModel.setArtifactId( STANDALONE_SUPERPOM_ARTIFACTID );

        superModel.setVersion( STANDALONE_SUPERPOM_VERSION );


        List activeProfiles;

        ProfileManager profileManager = config.getGlobalProfileManager();

        if ( profileManager == null )
        {
            profileManager = new DefaultProfileManager( container );
        }

        profileManager.addProfiles( superModel.getProfiles() );

        String projectId = safeVersionlessKey( STANDALONE_SUPERPOM_GROUPID, STANDALONE_SUPERPOM_ARTIFACTID );

        activeProfiles = injectActiveProfiles( profileManager, superModel );

        MavenProject project = new MavenProject( superModel );

        project.setManagedVersionMap(
            createManagedVersionMap( projectId, superModel.getDependencyManagement(), null ) );

        project.setActiveProfiles( activeProfiles );

        project.setOriginalModel( superModel );

        try
        {
            project = processProjectLogic( "<Super-POM>", project, config, null, null, true, true );

            project.setExecutionRoot( true );

            return project;
        }
        catch ( ModelInterpolationException e )
        {
            throw new ProjectBuildingException( projectId, e.getMessage(), e );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( projectId, e.getMessage(), e );
        }
    }

    public MavenProject buildWithDependencies( File projectDescriptor,
                                               ArtifactRepository localRepository,
                                               ProfileManager profileManager )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return buildWithDependencies( projectDescriptor, localRepository, profileManager, null );
    }

    // note:jvz This was added for the embedder.

    /** @todo move to metadatasource itself? */
    public MavenProject buildWithDependencies( File projectDescriptor,
                                               ArtifactRepository localRepository,
                                               ProfileManager profileManager,
                                               TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        MavenProject project = build( projectDescriptor, localRepository, profileManager, false );

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

        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
        Artifact projectArtifact = project.getArtifact();

        String projectId = safeVersionlessKey( project.getGroupId(), project.getArtifactId() );

        // Map managedVersions = createManagedVersionMap( projectId, project.getDependencyManagement() );
        Map managedVersions = project.getManagedVersionMap();

        ensureMetadataSourceIsInitialized();

        try
        {
            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ProjectBuildingException( projectId,
                                                "Unable to build project due to an invalid dependency version: " +
                                                    e.getMessage(), e );
        }

        if ( transferListener != null )
        {
            wagonManager.setDownloadMonitor( transferListener );
        }

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getDependencyArtifacts(),
                                                                                projectArtifact, managedVersions,
                                                                                localRepository,
                                                                                project.getRemoteArtifactRepositories(),
                                                                                artifactMetadataSource );

        project.setArtifacts( result.getArtifacts() );

        return project;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void ensureMetadataSourceIsInitialized()
        throws ProjectBuildingException
    {
        if ( artifactMetadataSource == null )
        {
            try
            {
                artifactMetadataSource = (ArtifactMetadataSource) container.lookup( ArtifactMetadataSource.ROLE );
            }
            catch ( ComponentLookupException e )
            {
                throw new ProjectBuildingException( "all", "Cannot lookup metadata source for building the project.",
                                                    e );
            }
        }
    }

    private Map createManagedVersionMap( String projectId,
                                         DependencyManagement dependencyManagement,
                                         MavenProject parent )
        throws ProjectBuildingException
    {
        Map map = null;
        List deps;
        if ( ( dependencyManagement != null ) && ( ( deps = dependencyManagement.getDependencies() ) != null ) &&
            ( deps.size() > 0 ) )
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

                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                                  versionRange, d.getType(),
                                                                                  d.getClassifier(), d.getScope(),
                                                                                  d.isOptional() );
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

                        Iterator exclItr = d.getExclusions().iterator();

                        while ( exclItr.hasNext() )
                        {
                            Exclusion e = (Exclusion) exclItr.next();
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
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + d.getVersion() +
                        "' for dependency '" + d.getManagementKey() + "': " + e.getMessage(), e );
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
                                                      ProjectBuilderConfiguration config,
                                                      boolean checkDistributionManagementStatus )
        throws ProjectBuildingException
    {
        Model model = readModel( "unknown", projectDescriptor, true );

        MavenProject project = buildInternal( projectDescriptor.getAbsolutePath(), model, config,
                                              buildArtifactRepositories( getSuperModel() ), projectDescriptor,
                                              true );

        if ( checkDistributionManagementStatus )
        {
            if ( ( project.getDistributionManagement() != null ) &&
                ( project.getDistributionManagement().getStatus() != null ) )
            {
                String projectId = safeVersionlessKey( project.getGroupId(), project.getArtifactId() );

                throw new ProjectBuildingException( projectId,
                                                    "Invalid project file: distribution status must not be specified for a project outside of the repository" );
            }
        }

        return project;
    }

    private Model findModelFromRepository( Artifact artifact,
                                           List remoteArtifactRepositories,
                                           ArtifactRepository localRepository,
                                           boolean allowStubModel )
        throws ProjectBuildingException
    {
        String projectId = safeVersionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        normalizeToArtifactRepositories( remoteArtifactRepositories, projectId );

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

            projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                     artifact.getVersion(), artifact.getScope() );
        }

        Model model;

        try
        {
            artifactResolver.resolve( projectArtifact, remoteArtifactRepositories, localRepository );

            File file = projectArtifact.getFile();

            model = readModel( projectId, file, false );

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
            throw new ProjectBuildingException( projectId, "Error getting POM for '" + projectId +
                "' from the repository: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            if ( allowStubModel )
            {
                getLogger().debug( "Artifact not found - using stub model: " + e.getMessage() );

                model = createStubModel( projectArtifact );
            }
            else
            {
                throw new ProjectBuildingException( projectId, "POM '" + projectId + "' not found in repository: " +
                    e.getMessage(), e );
            }
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
                    item = ProjectUtils.buildArtifactRepository( repo, artifactRepositoryFactory, container );

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
    // This is used when requested artifacts do not have an associated POM. This is for the case where we are
    // using an m1 repo where the only thing required to be present are the JAR files.
    private Model createStubModel( Artifact projectArtifact )
    {
        getLogger().debug( "Using defaults for missing POM " + projectArtifact );

        Model model = new Model();

        model.setModelVersion( "4.0.0" );

        model.setArtifactId( projectArtifact.getArtifactId() );

        model.setGroupId( projectArtifact.getGroupId() );

        model.setVersion( projectArtifact.getVersion() );

        // TODO: not correct in some instances
        model.setPackaging( projectArtifact.getType() );

        model.setDistributionManagement( new DistributionManagement() );

        model.getDistributionManagement().setStatus( ArtifactStatus.GENERATED.toString() );

        return model;
    }

    // jvz:note
    // We've got a mixture of things going in the USD and from the repository, sometimes the descriptor
    // is a real file and sometimes null which makes things confusing.
    private MavenProject buildInternal( String pomLocation,
                                        Model model,
                                        ProjectBuilderConfiguration config,
                                        List parentSearchRepositories,
                                        File projectDescriptor,
                                        boolean strict )
        throws ProjectBuildingException
    {
        File projectDir = null;

        if ( projectDescriptor != null )
        {
            projectDir = projectDescriptor.getAbsoluteFile().getParentFile();
        }

        Model superModel = getSuperModel();

        ProfileManager externalProfileManager = config.getGlobalProfileManager();
        ProfileManager superProjectProfileManager;
        if ( externalProfileManager != null )
        {
            superProjectProfileManager = new DefaultProfileManager(
                                                                    container,
                                                                    externalProfileManager.getRequestProperties() );
        }
        else
        {
            superProjectProfileManager = new DefaultProfileManager( container );
        }

        List activeProfiles;

        superProjectProfileManager.addProfiles( superModel.getProfiles() );

        activeProfiles = injectActiveProfiles( superProjectProfileManager, superModel );

        MavenProject superProject = new MavenProject( superModel );

        superProject.setActiveProfiles( activeProfiles );

        //noinspection CollectionDeclaredAsConcreteClass
        LinkedList lineage = new LinkedList();

        // TODO: the aRWR can get out of sync with project.model.repositories. We should do all the processing of
        // profiles, etc on the models then recreate the aggregated sets at the end from the project repositories (they
        // must still be created along the way so that parent poms can be discovered, however)
        // Use a TreeSet to ensure ordering is retained
        Set aggregatedRemoteWagonRepositories = new LinkedHashSet();

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        List activeExternalProfiles;
        try
        {
            if ( externalProfileManager != null )
            {
                activeExternalProfiles = externalProfileManager.getActiveProfiles();
            }
            else
            {
                activeExternalProfiles = Collections.EMPTY_LIST;
            }
        }
        catch ( ProfileActivationException e )
        {
            throw new ProjectBuildingException( projectId, "Failed to calculate active external profiles.", e );
        }

        for ( Iterator i = activeExternalProfiles.iterator(); i.hasNext(); )
        {
            Profile externalProfile = (Profile) i.next();

            for ( Iterator repoIterator = externalProfile.getRepositories().iterator(); repoIterator.hasNext(); )
            {
                Repository mavenRepo = (Repository) repoIterator.next();

                ArtifactRepository artifactRepo = null;
                try
                {
                    artifactRepo =
                        ProjectUtils.buildArtifactRepository( mavenRepo, artifactRepositoryFactory, container );
                }
                catch ( InvalidRepositoryException e )
                {
                    throw new ProjectBuildingException( projectId, e.getMessage(), e );
                }

                aggregatedRemoteWagonRepositories.add( artifactRepo );
            }
        }

        MavenProject project = null;
        try
        {
            project = assembleLineage( model, lineage, config, projectDir, parentSearchRepositories,
                                       aggregatedRemoteWagonRepositories, strict );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new ProjectBuildingException( projectId, e.getMessage(), e );
        }

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
                getLogger().debug( "Cannot determine whether " + currentProject.getId() + " is a module of " +
                    previousProject.getId() + ". Reason: " + e.getMessage(), e );
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
            project = processProjectLogic( pomLocation, project, config, projectDir, repositories, strict, false );
        }
        catch ( ModelInterpolationException e )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, e.getMessage(), e );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, e.getMessage(), e );
        }

        processedProjectCache.put(
                                  createCacheKey( project.getGroupId(), project.getArtifactId(), project.getVersion() ), project );

          // jvz:note
        // this only happens if we are building from a source file
        if ( projectDescriptor != null )
        {
            Build build = project.getBuild();

            project.addCompileSourceRoot( build.getSourceDirectory() );

            project.addScriptSourceRoot( build.getScriptSourceDirectory() );

            project.addTestCompileSourceRoot( build.getTestSourceDirectory() );

            // Only track the file of a POM in the source tree
            project.setFile( projectDescriptor );
        }

//        try
//        {
//            calculateConcreteState( project, config );
//        }
//        catch ( ModelInterpolationException e )
//        {
//            throw new InvalidProjectModelException( projectId, pomLocation, e.getMessage(), e );
//        }

        project.setManagedVersionMap( createManagedVersionMap( projectId,
                                                               project.getDependencyManagement(),
                                                               project.getParent() ) );

        return project;
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
            return ProjectUtils.buildArtifactRepositories( model.getRepositories(), artifactRepositoryFactory,
                                                           container );
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
    private MavenProject processProjectLogic( String pomLocation,
                                              MavenProject project,
                                              ProjectBuilderConfiguration config,
                                              File projectDir,
                                              List remoteRepositories,
                                              boolean strict,
                                              boolean isSuperPom )
        throws ProjectBuildingException, ModelInterpolationException, InvalidRepositoryException
    {
        Model model = project.getModel();

        List activeProfiles = project.getActiveProfiles();

        if ( activeProfiles == null )
        {
            activeProfiles = new ArrayList();
        }

        ProfileManager profileMgr = config == null ? null : config.getGlobalProfileManager();

        List injectedProfiles = injectActiveProfiles( profileMgr, model );

        activeProfiles.addAll( injectedProfiles );

        Build dynamicBuild = model.getBuild();

        model.setBuild( ModelUtils.cloneBuild( dynamicBuild ) );

        model = modelInterpolator.interpolate( model, projectDir, config, getLogger().isDebugEnabled() );

        mergeDeterministicBuildElements( model.getBuild(), dynamicBuild );

        model.setBuild( dynamicBuild );

        // MNG-3482: Make sure depMgmt is interpolated before merging.
        if ( !isSuperPom )
        {
            mergeManagedDependencies( model, config.getLocalRepository(), remoteRepositories );
        }

        // interpolation is before injection, because interpolation is off-limits in the injected variables
        modelDefaultsInjector.injectDefaults( model );

        MavenProject parentProject = project.getParent();

        Model originalModel = project.getOriginalModel();

        // We will return a different project object using the new model (hence the need to return a project, not just modify the parameter)
        project = new MavenProject( model );

        project.setOriginalModel( originalModel );

        project.setActiveProfiles( activeProfiles );

        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        Artifact projectArtifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                                        project.getVersion(), project.getPackaging() );

        project.setArtifact( projectArtifact );

        project.setPluginArtifactRepositories( ProjectUtils.buildArtifactRepositories( model.getPluginRepositories(),
                                                                                       artifactRepositoryFactory,
                                                                                       container ) );

        DistributionManagement dm = model.getDistributionManagement();
        if ( dm != null )
        {
            ArtifactRepository repo = ProjectUtils.buildDeploymentArtifactRepository( dm.getRepository(),
                                                                                      artifactRepositoryFactory,
                                                                                      container );
            project.setReleaseArtifactRepository( repo );

            if ( dm.getSnapshotRepository() != null )
            {
                repo = ProjectUtils.buildDeploymentArtifactRepository( dm.getSnapshotRepository(),
                                                                       artifactRepositoryFactory, container );
                project.setSnapshotArtifactRepository( repo );
            }
        }

        if ( parentProject != null )
        {
            String cacheKey = createCacheKey( parentProject.getGroupId(),
                                              parentProject.getArtifactId(),
                                              parentProject.getVersion() );

            MavenProject processedParent = (MavenProject) processedProjectCache.get( cacheKey );
            Artifact parentArtifact;

            // yeah, this null check might be a bit paranoid, but better safe than sorry...
            if ( processedParent != null )
            {
                project.setParent( processedParent );

                parentArtifact = processedParent.getArtifact();
            }
            else
            {
                project.setParent( parentProject );

                parentArtifact = artifactFactory.createParentArtifact( parentProject.getGroupId(),
                                                                                parentProject.getArtifactId(),
                                                                                parentProject.getVersion() );
            }

            project.setParentArtifact( parentArtifact );
        }

        // Must validate before artifact construction to make sure dependencies are good
        ModelValidationResult validationResult = validator.validate( model );

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, "Failed to validate POM",
                                                    validationResult );
        }

        project.setRemoteArtifactRepositories(
            ProjectUtils.buildArtifactRepositories( model.getRepositories(), artifactRepositoryFactory, container ) );

        // TODO: these aren't taking active project artifacts into consideration in the reactor
        project.setPluginArtifacts( createPluginArtifacts( projectId, project.getBuildPlugins() ) );

        project.setReportArtifacts( createReportArtifacts( projectId, project.getReportPlugins() ) );

        project.setExtensionArtifacts( createExtensionArtifacts( projectId, project.getBuildExtensions() ) );

        return project;
    }

    private void mergeDeterministicBuildElements( Build interpolatedBuild,
                                                  Build dynamicBuild )
    {
        List dPlugins = dynamicBuild.getPlugins();

        if ( dPlugins != null )
        {
            List iPlugins = interpolatedBuild.getPlugins();

            for ( int i = 0; i < dPlugins.size(); i++ )
            {
                Plugin dPlugin = (Plugin) dPlugins.get( i );
                Plugin iPlugin = (Plugin) iPlugins.get( i );

                dPlugin.setGroupId( iPlugin.getGroupId() );
                dPlugin.setArtifactId( iPlugin.getArtifactId() );
                dPlugin.setVersion( iPlugin.getVersion() );
            }
        }

        PluginManagement dPluginMgmt = dynamicBuild.getPluginManagement();

        if ( dPluginMgmt != null )
        {
            PluginManagement iPluginMgmt = interpolatedBuild.getPluginManagement();
            dPlugins = dPluginMgmt.getPlugins();
            if ( dPlugins != null )
            {
                List iPlugins = iPluginMgmt.getPlugins();

                for ( int i = 0; i < dPlugins.size(); i++ )
                {
                    Plugin dPlugin = (Plugin) dPlugins.get( i );
                    Plugin iPlugin = (Plugin) iPlugins.get( i );

                    dPlugin.setGroupId( iPlugin.getGroupId() );
                    dPlugin.setArtifactId( iPlugin.getArtifactId() );
                    dPlugin.setVersion( iPlugin.getVersion() );
                }
            }
        }

        if ( dynamicBuild.getExtensions() != null )
        {
            dynamicBuild.setExtensions( interpolatedBuild.getExtensions() );
        }
    }

    /**
     * @noinspection CollectionDeclaredAsConcreteClass
     * @todo We need to find an effective way to unit test parts of this method!
     * @todo Refactor this into smaller methods with discrete purposes.
     */
    private MavenProject assembleLineage( Model model,
                                          LinkedList lineage,
                                          ProjectBuilderConfiguration config,
                                          File projectDir,
                                          List parentSearchRepositories,
                                          Set aggregatedRemoteWagonRepositories,
                                          boolean strict )
        throws ProjectBuildingException, InvalidRepositoryException
    {
        Model originalModel = ModelUtils.cloneModel( model );


        if ( !model.getRepositories().isEmpty() )
        {
            List respositories = buildArtifactRepositories( model );

            for ( Iterator it = respositories.iterator(); it.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) it.next();

                if ( !aggregatedRemoteWagonRepositories.contains( repository ) )
                {
                    aggregatedRemoteWagonRepositories.add( repository );
                }
            }
        }

        ProfileManager externalProfileManager = config.getGlobalProfileManager();
        ProfileManager profileManager;
        if ( externalProfileManager != null )
        {
            profileManager = new DefaultProfileManager( container, externalProfileManager.getRequestProperties() );
        }
        else
        {
            //TODO mkleint - use the (Container, Properties constructor to make system properties embeddable
            profileManager = new DefaultProfileManager( container );
        }

        if ( externalProfileManager != null )
        {
            profileManager.explicitlyActivate( externalProfileManager.getExplicitlyActivatedIds() );

            profileManager.explicitlyDeactivate( externalProfileManager.getExplicitlyDeactivatedIds() );
        }

        List activeProfiles;

        try
        {
            profileManager.addProfiles( model.getProfiles() );

            loadProjectExternalProfiles( profileManager, projectDir );

            activeProfiles = injectActiveProfiles( profileManager, model );
        }
        catch ( ProfileActivationException e )
        {
            String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

            throw new ProjectBuildingException( projectId, "Failed to activate local (project-level) build profiles: " +
                e.getMessage(), e );
        }

        MavenProject project = new MavenProject( model );

        project.setActiveProfiles( activeProfiles );
        project.setOriginalModel( originalModel );

        lineage.addFirst( project );

        Parent parentModel = model.getParent();

        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        if ( parentModel != null )
        {
            if ( StringUtils.isEmpty( parentModel.getGroupId() ) )
            {
                throw new ProjectBuildingException( projectId, "Missing groupId element from parent element" );
            }
            else if ( StringUtils.isEmpty( parentModel.getArtifactId() ) )
            {
                throw new ProjectBuildingException( projectId, "Missing artifactId element from parent element" );
            }
            else if ( parentModel.getGroupId().equals( model.getGroupId() ) &&
                parentModel.getArtifactId().equals( model.getArtifactId() ) )
            {
                throw new ProjectBuildingException( projectId,
                                                    "Parent element is a duplicate of " + "the current project " );
            }
            else if ( StringUtils.isEmpty( parentModel.getVersion() ) )
            {
                throw new ProjectBuildingException( projectId, "Missing version element from parent element" );
            }

            // the only way this will have a value is if we find the parent on disk...
            File parentDescriptor = null;

            model = null;

            String parentKey =
                createCacheKey( parentModel.getGroupId(), parentModel.getArtifactId(), parentModel.getVersion() );
            MavenProject parentProject = (MavenProject) rawProjectCache.get( parentKey );

            if ( parentProject != null )
            {
                model = ModelUtils.cloneModel( parentProject.getModel() );

                parentDescriptor = parentProject.getFile();
            }

            String parentRelativePath = parentModel.getRelativePath();

            // if we can't find a cached model matching the parent spec, then let's try to look on disk using
            // <relativePath/>
            if ( ( model == null ) && ( projectDir != null ) && StringUtils.isNotEmpty( parentRelativePath ) )
            {
                parentDescriptor = new File( projectDir, parentRelativePath );

                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Searching for parent-POM: " + parentModel.getId() + " of project: " +
                        project.getId() + " in relative path: " + parentRelativePath );
                }

                if ( parentDescriptor.isDirectory() )
                {
                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Path specified in <relativePath/> (" + parentRelativePath +
                            ") is a directory. Searching for 'pom.xml' within this directory." );
                    }

                    parentDescriptor = new File( parentDescriptor, "pom.xml" );

                    if ( !parentDescriptor.exists() )
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug( "Parent-POM: " + parentModel.getId() + " for project: " +
                                project.getId() + " cannot be loaded from relative path: " + parentDescriptor +
                                "; path does not exist." );
                        }
                    }
                }

                if ( parentDescriptor != null )
                {
                    try
                    {
                        parentDescriptor = parentDescriptor.getCanonicalFile();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Failed to canonicalize potential parent POM: \'" + parentDescriptor + "\'",
                                           e );

                        parentDescriptor = null;
                    }
                }

                if ( ( parentDescriptor != null ) && parentDescriptor.exists() )
                {
                    Model candidateParent = readModel( projectId, parentDescriptor, strict );

                    String candidateParentGroupId = candidateParent.getGroupId();
                    if ( ( candidateParentGroupId == null ) && ( candidateParent.getParent() != null ) )
                    {
                        candidateParentGroupId = candidateParent.getParent().getGroupId();
                    }

                    String candidateParentVersion = candidateParent.getVersion();
                    if ( ( candidateParentVersion == null ) && ( candidateParent.getParent() != null ) )
                    {
                        candidateParentVersion = candidateParent.getParent().getVersion();
                    }

                    if ( parentModel.getGroupId().equals( candidateParentGroupId ) &&
                        parentModel.getArtifactId().equals( candidateParent.getArtifactId() ) &&
                        parentModel.getVersion().equals( candidateParentVersion ) )
                    {
                        model = candidateParent;

                        getLogger().debug( "Using parent-POM from the project hierarchy at: \'" +
                            parentModel.getRelativePath() + "\' for project: " + project.getId() );
                    }
                    else
                    {
                        getLogger().debug( "Invalid parent-POM referenced by relative path '" +
                            parentModel.getRelativePath() + "' in parent specification in " + project.getId() + ":" +
                            "\n  Specified: " + parentModel.getId() + "\n  Found:     " + candidateParent.getId() );
                    }
                }
                else if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Parent-POM: " + parentModel.getId() + " not found in relative path: " + parentRelativePath );
                }
            }

            Artifact parentArtifact = null;

            // only resolve the parent model from the repository system if we didn't find it on disk...
            if ( model == null )
            {
                // MNG-2302: parent's File was being populated incorrectly when parent is loaded from repo.
                // keep this in line with other POMs loaded from the repository...the file should be null.
                parentDescriptor = null;

                //!! (**)
                // ----------------------------------------------------------------------
                // Do we have the necessary information to actually find the parent
                // POMs here?? I don't think so ... Say only one remote repository is
                // specified and that is ibiblio then this model that we just read doesn't
                // have any repository information ... I think we might have to inherit
                // as we go in order to do this.
                // ----------------------------------------------------------------------

                // we must add the repository this POM was found in too, by chance it may be located where the parent is
                // we can't query the parent to ask where it is :)
                List remoteRepositories = new ArrayList( aggregatedRemoteWagonRepositories );
                remoteRepositories.addAll( parentSearchRepositories );

                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Retrieving parent-POM: " + parentModel.getId() + " for project: " +
                        project.getId() + " from the repository." );
                }

                parentArtifact = artifactFactory.createParentArtifact( parentModel.getGroupId(),
                                                                       parentModel.getArtifactId(),
                                                                       parentModel.getVersion() );

                try
                {
                    model = findModelFromRepository( parentArtifact, remoteRepositories, config.getLocalRepository(), false );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new ProjectBuildingException( project.getId(), "Cannot find parent: " + e.getProjectId() +
                        " for project: " + project.getId(), e );
                }
            }

            if ( ( model != null ) && !"pom".equals( model.getPackaging() ) )
            {
                throw new ProjectBuildingException( projectId, "Parent: " + model.getId() + " of project: " +
                    projectId + " has wrong packaging: " + model.getPackaging() + ". Must be 'pom'." );
            }

            File parentProjectDir = null;
            if ( parentDescriptor != null )
            {
                parentProjectDir = parentDescriptor.getParentFile();
            }

            MavenProject parent = assembleLineage( model,
                                                   lineage,
                                                   config,
                                                   parentProjectDir,
                                                   parentSearchRepositories,
                                                   aggregatedRemoteWagonRepositories,
                                                   strict );

            parent.setFile( parentDescriptor );

            project.setParent( parent );

            project.setParentArtifact( parentArtifact );
        }

        rawProjectCache.put( createCacheKey( project.getGroupId(), project.getArtifactId(), project.getVersion() ), new MavenProject( project ) );

        return project;
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
                if ( dep.getType().equals( "pom" ) && Artifact.SCOPE_IMPORT.equals( dep.getScope() ) )
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
                    if ( dep.getType().equals( "pom" )
                         && Artifact.SCOPE_IMPORT.equals( dep.getScope() ) )
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

    private List injectActiveProfiles( ProfileManager profileManager,
                                       Model model )
        throws ProjectBuildingException
    {
        List activeProfiles;

        if ( profileManager != null )
        {
            try
            {
                activeProfiles = profileManager.getActiveProfiles();
            }
            catch ( ProfileActivationException e )
            {
                String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

                throw new ProjectBuildingException( projectId, e.getMessage(), e );
            }

            for ( Iterator it = activeProfiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                profileInjector.inject( profile, model );
            }
        }
        else
        {
            activeProfiles = Collections.EMPTY_LIST;
        }

        return activeProfiles;
    }

    private void loadProjectExternalProfiles( ProfileManager profileManager,
                                              File projectDir )
        throws ProfileActivationException
    {
        if ( projectDir != null )
        {
            try
            {
                ProfilesRoot root = profilesBuilder.buildProfiles( projectDir );

                if ( root != null )
                {
                    List active = root.getActiveProfiles();

                    if ( ( active != null ) && !active.isEmpty() )
                    {
                        profileManager.explicitlyActivate( root.getActiveProfiles() );
                    }

                    for ( Iterator it = root.getProfiles().iterator(); it.hasNext(); )
                    {
                        org.apache.maven.profiles.Profile rawProfile = (org.apache.maven.profiles.Profile) it.next();

                        Profile converted = ProfilesConversionUtils.convertFromProfileXmlProfile( rawProfile );

                        profileManager.addProfile( converted );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new ProfileActivationException( "Cannot read profiles.xml resource from directory: " + projectDir,
                                                      e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ProfileActivationException(
                    "Cannot parse profiles.xml resource from directory: " + projectDir, e );
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
            return readModel( projectId, file.getAbsolutePath(), reader, strict );
        }
        catch ( FileNotFoundException e )
        {
            throw new ProjectBuildingException( projectId,
                                                "Could not find the model file '" + file.getAbsolutePath() + "'.", e );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( projectId, "Failed to build model from file '" +
                file.getAbsolutePath() + "'.\nError: \'" + e.getLocalizedMessage() + "\'", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private Model readModel( String projectId,
                             String pomLocation,
                             Reader reader,
                             boolean strict )
        throws IOException, InvalidProjectModelException
    {
        String modelSource = IOUtil.toString( reader );

        if ( modelSource.indexOf( "<modelVersion>" + MAVEN_MODEL_VERSION ) < 0 )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, "Not a v" + MAVEN_MODEL_VERSION  + " POM." );
        }

        StringReader sReader = new StringReader( modelSource );

        try
        {
            return modelReader.read( sReader, strict );
        }
        catch ( XmlPullParserException e )
        {
            throw new InvalidProjectModelException( projectId, pomLocation,
                                                    "Parse error reading POM. Reason: " + e.getMessage(), e );
        }
    }

    private Model readModel( String projectId,
                             URL url,
                             boolean strict )
        throws ProjectBuildingException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( url.openStream() );
            return readModel( projectId, url.toExternalForm(), reader, strict );
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

    private static String createCacheKey( String groupId,
                                          String artifactId,
                                          String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    protected Set createPluginArtifacts( String projectId,
                                         List plugins )
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
                throw new ProjectBuildingException( projectId, "Unable to parse version '" + version +
                    "' for plugin '" + ArtifactUtils.versionlessKey( p.getGroupId(), p.getArtifactId() ) + "': " +
                    e.getMessage(), e );
            }

            if ( artifact != null )
            {
                pluginArtifacts.add( artifact );
            }
        }

        return pluginArtifacts;
    }

    // TODO: share with createPluginArtifacts?
    protected Set createReportArtifacts( String projectId,
                                         List reports )
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
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + version +
                        "' for report '" + ArtifactUtils.versionlessKey( p.getGroupId(), p.getArtifactId() ) + "': " +
                        e.getMessage(), e );
                }

                if ( artifact != null )
                {
                    pluginArtifacts.add( artifact );
                }
            }
        }

        return pluginArtifacts;
    }

    // TODO: share with createPluginArtifacts?
    protected Set createExtensionArtifacts( String projectId,
                                            List extensions )
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
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + version +
                        "' for extension '" + ArtifactUtils.versionlessKey( ext.getGroupId(), ext.getArtifactId() ) +
                        "': " + e.getMessage(), e );
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

    private Model getSuperModel()
        throws ProjectBuildingException
    {
        URL url = DefaultMavenProjectBuilder.class.getResource( "pom-" + MAVEN_MODEL_VERSION + ".xml" );

        String projectId = safeVersionlessKey( STANDALONE_SUPERPOM_GROUPID, STANDALONE_SUPERPOM_ARTIFACTID );

        return readModel( projectId, url, true );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void calculateConcreteState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( project.isConcrete() )
        {
            return;
        }

        Build build = project.getBuild();
        if ( build != null )
        {
            initResourceMergeIds( build.getResources() );
            initResourceMergeIds( build.getTestResources() );
        }

        Model model = ModelUtils.cloneModel( project.getModel() );

        // We don't need all the project methods that are added over those in the model, but we do need basedir
        Map context = new HashMap();

        File basedir = project.getBasedir();

        model = modelInterpolator.interpolate( model, project.getBasedir(), config, getLogger().isDebugEnabled() );

        List originalInterpolatedCompileSourceRoots = interpolateListOfStrings( project.getCompileSourceRoots(),
                                                                           model,
                                                                           project.getBasedir(),
                                                                           config,
                                                                           getLogger().isDebugEnabled() );

        project.preserveCompileSourceRoots( originalInterpolatedCompileSourceRoots );

        project.setCompileSourceRoots( originalInterpolatedCompileSourceRoots == null ? null
                        : translateListOfPaths( originalInterpolatedCompileSourceRoots, basedir ) );

        List originalInterpolatedTestCompileSourceRoots = interpolateListOfStrings( project.getTestCompileSourceRoots(),
                                                                               model,
                                                                               project.getBasedir(),
                                                                               config,
                                                                               getLogger().isDebugEnabled() );

        project.preserveTestCompileSourceRoots( originalInterpolatedTestCompileSourceRoots );
        project.setTestCompileSourceRoots( originalInterpolatedTestCompileSourceRoots == null ? null
                        : translateListOfPaths( originalInterpolatedTestCompileSourceRoots, basedir ) );

        List originalInterpolatedScriptSourceRoots = interpolateListOfStrings( project.getScriptSourceRoots(),
                                                                          model,
                                                                          project.getBasedir(),
                                                                          config,
                                                                          getLogger().isDebugEnabled() );

        project.preserveScriptSourceRoots( originalInterpolatedScriptSourceRoots );
        project.setScriptSourceRoots( originalInterpolatedScriptSourceRoots == null ? null
                        : translateListOfPaths( originalInterpolatedScriptSourceRoots, basedir ) );

        Model model2 = ModelUtils.cloneModel( model );

        if ( basedir != null )
        {
            pathTranslator.alignToBaseDirectory( model, basedir );
        }

        project.preserveBuild( model2.getBuild() );
        project.setBuild( model.getBuild() );

        calculateConcreteProjectReferences( project, config );

        if ( project.getExecutionProject() != null )
        {
            calculateConcreteState( project.getExecutionProject(), config );
        }

        project.setConcrete( true );
    }

    private void initResourceMergeIds( List resources )
    {
        if ( resources != null )
        {
            for ( Iterator it = resources.iterator(); it.hasNext(); )
            {
                Resource resource = (Resource) it.next();

                resource.initMergeId();
            }
        }
    }

    private void calculateConcreteProjectReferences( MavenProject project,
                                                     ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        Map projectRefs = project.getProjectReferences();

        if ( projectRefs != null )
        {
            for ( Iterator it = projectRefs.values().iterator(); it.hasNext(); )
            {
                MavenProject reference = (MavenProject) it.next();
                calculateConcreteState( reference, config );
            }
        }
    }

    private List translateListOfPaths( List paths, File basedir )
    {
        if ( paths == null )
        {
            return null;
        }
        else if ( basedir == null )
        {
            return paths;
        }

        List result = new ArrayList( paths.size() );
        for ( Iterator it = paths.iterator(); it.hasNext(); )
        {
            String path = (String) it.next();

            String aligned = pathTranslator.alignToBaseDirectory( path, basedir );

            result.add( aligned );
        }

        return result;
    }

    public void restoreDynamicState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( !project.isConcrete() )
        {
            return;
        }

        restoreBuildRoots( project, config, getLogger().isDebugEnabled() );
        restoreModelBuildSection( project, config, getLogger().isDebugEnabled() );

        restoreDynamicProjectReferences( project, config );
        if ( project.getExecutionProject() != null )
        {
            restoreDynamicState( project.getExecutionProject(), config );
        }

        project.setConcrete( false );
    }

    private void restoreDynamicProjectReferences( MavenProject project,
                                                  ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        Map projectRefs = project.getProjectReferences();
        if ( projectRefs != null )
        {
            for ( Iterator it = projectRefs.values().iterator(); it.hasNext(); )
            {
                MavenProject projectRef = (MavenProject) it.next();
                restoreDynamicState( projectRef, config );
            }
        }
    }

    private void restoreBuildRoots( MavenProject project,
                                    ProjectBuilderConfiguration config,
                                    boolean debugMessages )
        throws ModelInterpolationException
    {
        project.setCompileSourceRoots( restoreListOfStrings( project.getDynamicCompileSourceRoots(),
                                                             project.getOriginalInterpolatedCompileSourceRoots(),
                                                             project.getCompileSourceRoots(),
                                                             project,
                                                             config,
                                                             debugMessages ) );

        project.setTestCompileSourceRoots( restoreListOfStrings( project.getDynamicTestCompileSourceRoots(),
                                                                 project.getOriginalInterpolatedTestCompileSourceRoots(),
                                                                 project.getTestCompileSourceRoots(),
                                                                 project,
                                                                 config,
                                                                 debugMessages ) );

        project.setScriptSourceRoots( restoreListOfStrings( project.getDynamicScriptSourceRoots(),
                                                            project.getOriginalInterpolatedScriptSourceRoots(),
                                                            project.getScriptSourceRoots(),
                                                            project,
                                                            config,
                                                            debugMessages ) );

        project.clearRestorableRoots();
    }

    private void restoreModelBuildSection( MavenProject project,
                                           ProjectBuilderConfiguration config,
                                           boolean debugMessages )
        throws ModelInterpolationException
    {
        Build changedBuild = project.getBuild();
        Build dynamicBuild = project.getDynamicBuild();
        Build originalInterpolatedBuild = project.getOriginalInterpolatedBuild();

        dynamicBuild.setResources( restoreResources( dynamicBuild.getResources(),
                                                         originalInterpolatedBuild.getResources(),
                                                         changedBuild.getResources(),
                                                         project,
                                                         config,
                                                         debugMessages ) );

        dynamicBuild.setTestResources( restoreResources( dynamicBuild.getTestResources(),
                                                         originalInterpolatedBuild.getTestResources(),
                                                         changedBuild.getTestResources(),
                                                         project,
                                                         config,
                                                         debugMessages ) );

        dynamicBuild.setFilters( restoreListOfStrings( dynamicBuild.getFilters(),
                                                           originalInterpolatedBuild.getFilters(),
                                                           changedBuild.getFilters(),
                                                           project,
                                                           config,
                                                           debugMessages ) );

        dynamicBuild.setFinalName( restoreString( dynamicBuild.getFinalName(),
                                                  originalInterpolatedBuild.getFinalName(),
                                                  changedBuild.getFinalName(),
                                                  project,
                                                  config,
                                                  debugMessages ) );

        dynamicBuild.setDefaultGoal( restoreString( dynamicBuild.getDefaultGoal(),
                                                  originalInterpolatedBuild.getDefaultGoal(),
                                                  changedBuild.getDefaultGoal(),
                                                  project,
                                                  config,
                                                  debugMessages ) );

        dynamicBuild.setSourceDirectory( restoreString( dynamicBuild.getSourceDirectory(),
                                                            originalInterpolatedBuild.getSourceDirectory(),
                                                            changedBuild.getSourceDirectory(),
                                                            project,
                                                            config,
                                                            debugMessages ) );

        dynamicBuild.setTestSourceDirectory( restoreString( dynamicBuild.getTestSourceDirectory(),
                                                                originalInterpolatedBuild.getTestSourceDirectory(),
                                                                changedBuild.getTestSourceDirectory(),
                                                                project,
                                                                config,
                                                                debugMessages ) );

        dynamicBuild.setScriptSourceDirectory( restoreString( dynamicBuild.getScriptSourceDirectory(),
                                                                  originalInterpolatedBuild.getScriptSourceDirectory(),
                                                                  changedBuild.getScriptSourceDirectory(),
                                                                  project,
                                                                  config,
                                                                  debugMessages ) );

        dynamicBuild.setOutputDirectory( restoreString( dynamicBuild.getOutputDirectory(),
                                                            originalInterpolatedBuild.getOutputDirectory(),
                                                            changedBuild.getOutputDirectory(),
                                                            project,
                                                            config,
                                                            debugMessages ) );

        dynamicBuild.setTestOutputDirectory( restoreString( dynamicBuild.getTestOutputDirectory(),
                                                                originalInterpolatedBuild.getTestOutputDirectory(),
                                                                changedBuild.getTestOutputDirectory(),
                                                                project,
                                                                config,
                                                                debugMessages ) );

        dynamicBuild.setDirectory( restoreString( dynamicBuild.getDirectory(),
                                                      originalInterpolatedBuild.getDirectory(),
                                                      changedBuild.getDirectory(),
                                                      project,
                                                      config,
                                                      debugMessages ) );

        project.setBuild( dynamicBuild );

        project.clearRestorableBuild();
    }

    private List interpolateListOfStrings( List originalStrings,
                                           Model model,
                                           File projectDir,
                                           ProjectBuilderConfiguration config,
                                           boolean debugMessages )
        throws ModelInterpolationException
    {
        if ( originalStrings == null )
        {
            return null;
        }

        List result = new ArrayList();

        for ( Iterator it = originalStrings.iterator(); it.hasNext(); )
        {
            String original = (String) it.next();
            String interpolated = modelInterpolator.interpolate( original, model, projectDir, config, debugMessages );

            result.add( interpolated );
        }

        return result;
    }

    private String restoreString( String originalString,
                                      String originalInterpolatedString,
                                      String changedString,
                                      MavenProject project,
                                      ProjectBuilderConfiguration config,
                                      boolean debugMessages )
        throws ModelInterpolationException
    {
        if ( originalString == null )
        {
            return changedString;
        }
        else if ( changedString == null )
        {
            return originalString;
        }

        Model model = project.getModel();

        String relativeChangedString;
        if ( project.getBasedir() != null )
        {
            relativeChangedString = pathTranslator.unalignFromBaseDirectory( changedString, project.getBasedir() );
        }
        else
        {
            relativeChangedString = changedString;
        }

        String interpolatedOriginal = modelInterpolator.interpolate( originalString,
                                                                     model,
                                                                     project.getBasedir(),
                                                                     config,
                                                                     debugMessages );
        
        interpolatedOriginal = pathTranslator.unalignFromBaseDirectory( interpolatedOriginal, project.getBasedir() );

        String interpolatedOriginal2 = modelInterpolator.interpolate( originalInterpolatedString,
                                                                      model,
                                                                      project.getBasedir(),
                                                                      config,
                                                                      debugMessages );
        
        interpolatedOriginal2 = pathTranslator.alignToBaseDirectory( interpolatedOriginal2, project.getBasedir() );

        String interpolatedChanged = modelInterpolator.interpolate( changedString,
                                                                    model,
                                                                    project.getBasedir(),
                                                                    config,
                                                                    debugMessages );
        
        interpolatedChanged = pathTranslator.alignToBaseDirectory( interpolatedChanged, project.getBasedir() );

        String relativeInterpolatedChanged = modelInterpolator.interpolate( relativeChangedString,
                                                                            model,
                                                                            project.getBasedir(),
                                                                            config,
                                                                            debugMessages );

        if ( interpolatedOriginal.equals( interpolatedChanged ) || interpolatedOriginal2.equals( interpolatedChanged ) )
        {
            return originalString;
        }
        else if ( interpolatedOriginal.equals( relativeInterpolatedChanged )
            || interpolatedOriginal2.equals( relativeInterpolatedChanged ) )
        {
            return originalString;
        }

        return relativeChangedString;
    }

    private List restoreListOfStrings( List originalStrings,
                                           List originalInterpolatedStrings,
                                           List changedStrings,
                                           MavenProject project,
                                           ProjectBuilderConfiguration config,
                                           boolean debugMessages )
        throws ModelInterpolationException
    {
        if ( originalStrings == null )
        {
            return changedStrings;
        }
        else if ( changedStrings == null )
        {
            return originalStrings;
        }

        List result = new ArrayList();

        Map orig = new HashMap();
        for ( int idx = 0; idx < originalStrings.size(); idx++ )
        {
            String[] permutations = new String[2];

            permutations[0] = pathTranslator.alignToBaseDirectory( (String) originalInterpolatedStrings.get( idx ), project.getBasedir() );
            permutations[1] = (String) originalStrings.get( idx );

            orig.put( permutations[0], permutations );
        }

        for ( Iterator it = changedStrings.iterator(); it.hasNext(); )
        {
            String changedString = (String) it.next();
            String relativeChangedString;
            if ( project.getBasedir() != null )
            {
                relativeChangedString = pathTranslator.unalignFromBaseDirectory( changedString, project.getBasedir() );
            }
            else
            {
                relativeChangedString = changedString;
            }

            String interpolated = modelInterpolator.interpolate( changedString,
                                                                 project.getModel(),
                                                                 project.getBasedir(),
                                                                 config,
                                                                 debugMessages );
            
            interpolated = pathTranslator.alignToBaseDirectory( interpolated, project.getBasedir() );

            String relativeInterpolated = modelInterpolator.interpolate( relativeChangedString,
                                                                         project.getModel(),
                                                                         project.getBasedir(),
                                                                         config,
                                                                         debugMessages );
            
            String[] original = (String[]) orig.get( interpolated );
            if ( original == null )
            {
                original = (String[]) orig.get( relativeInterpolated );
            }

            if ( original == null )
            {
                result.add( relativeChangedString );
            }
            else
            {
                result.add( original[1] );
            }
        }

        return result;
    }

    private List restoreResources( List originalResources,
                                       List originalInterpolatedResources,
                                       List changedResources,
                                       MavenProject project,
                                       ProjectBuilderConfiguration config,
                                       boolean debugMessages )
        throws ModelInterpolationException
    {
        if ( originalResources == null || changedResources == null )
        {
            return originalResources;
        }

        List result = new ArrayList();

        Map originalResourcesByMergeId = new HashMap();
        for ( int idx = 0; idx < originalResources.size(); idx++ )
        {
            Resource[] permutations = new Resource[2];

            permutations[0] = (Resource) originalInterpolatedResources.get( idx );
            permutations[1] = (Resource) originalResources.get( idx );

            originalResourcesByMergeId.put( permutations[0].getMergeId(), permutations );
        }

        for ( Iterator it = changedResources.iterator(); it.hasNext(); )
        {
            Resource resource = (Resource) it.next();
            String mergeId = resource.getMergeId();
            if ( mergeId == null || !originalResourcesByMergeId.containsKey( mergeId ) )
            {
                result.add( resource );
            }
            else
            {
                Resource originalInterpolatedResource = ( (Resource[]) originalResourcesByMergeId.get( mergeId ) )[0];
                Resource originalResource = ( (Resource[]) originalResourcesByMergeId.get( mergeId ) )[1];

                String dir = modelInterpolator.interpolate( resource.getDirectory(), project.getModel(), project.getBasedir(), config, getLogger().isDebugEnabled() );
                String oDir = originalInterpolatedResource.getDirectory();

                if ( !dir.equals( oDir ) )
                {
                    originalResource.setDirectory( pathTranslator.unalignFromBaseDirectory( dir, project.getBasedir() ) );
                }

                if ( resource.getTargetPath() != null )
                {
                    String target = modelInterpolator.interpolate( resource.getTargetPath(), project.getModel(), project.getBasedir(), config, getLogger().isDebugEnabled() );

                    String oTarget = originalInterpolatedResource.getTargetPath();

                    if ( !target.equals( oTarget ) )
                    {
                        originalResource.setTargetPath( pathTranslator.unalignFromBaseDirectory( target, project.getBasedir() ) );
                    }
                }

                originalResource.setFiltering( resource.isFiltering() );

                originalResource.setExcludes( collectRestoredListOfPatterns( resource.getExcludes(),
                                                                             originalResource.getExcludes(),
                                                                             originalInterpolatedResource.getExcludes() ) );

                originalResource.setIncludes( collectRestoredListOfPatterns( resource.getIncludes(),
                                                                             originalResource.getIncludes(),
                                                                             originalInterpolatedResource.getIncludes() ) );

                result.add( originalResource );
            }
        }

        return result;
    }

    private List collectRestoredListOfPatterns( List patterns,
                                                        List originalPatterns,
                                                        List originalInterpolatedPatterns )
    {
        LinkedHashSet collectedPatterns = new LinkedHashSet();

        collectedPatterns.addAll( originalPatterns );

        for ( Iterator it = patterns.iterator(); it.hasNext(); )
        {
            String pattern = (String) it.next();
            if ( !originalInterpolatedPatterns.contains( pattern ) )
            {
                collectedPatterns.add( pattern );
            }
        }

        return collectedPatterns.isEmpty() ? Collections.EMPTY_LIST
                        : new ArrayList( collectedPatterns );
    }

}
