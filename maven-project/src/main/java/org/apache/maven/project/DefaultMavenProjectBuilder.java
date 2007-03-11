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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.build.ProjectBuildCache;
import org.apache.maven.project.build.ProjectBuildContext;
import org.apache.maven.project.build.model.DefaultModelLineage;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.build.model.ModelLineageIterator;
import org.apache.maven.project.build.profile.ProfileAdvisor;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.injection.ModelDefaultsInjector;
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
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
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
 * @version $Id: DefaultMavenProjectBuilder.java,v 1.37 2005/03/08 01:55:22
 *          trygvis Exp $
 */
public class DefaultMavenProjectBuilder
    extends AbstractLogEnabled
    implements MavenProjectBuilder, Initializable, Contextualizable
{
    protected PlexusContainer container;

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
    
    private BuildContextManager buildContextManager;

    private MavenTools mavenTools;

    // ----------------------------------------------------------------------
    // I am making this available for use with a new method that takes a
    // a monitor wagon monitor as a parameter so that tools can use the
    // methods here and receive callbacks. MNG-1015
    //     
    //    Probably no longer relevant with wagonManager/wagonManager change - joakime
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

    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        return buildFromSourceFileInternal( projectDescriptor, localRepository, profileManager, true );
    }

    public MavenProject build( File projectDescriptor,
                               ArtifactRepository localRepository,
                               ProfileManager profileManager,
                               boolean checkDistributionManagementStatus )
        throws ProjectBuildingException
    {
        return buildFromSourceFileInternal( projectDescriptor, localRepository, profileManager, checkDistributionManagementStatus );
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
        ProjectBuildCache projectBuildCache = ProjectBuildCache.read( buildContextManager );
        
        MavenProject project = (MavenProject) projectBuildCache.getCachedProject( artifact );

        if ( project != null )
        {
            return project;
        }

        Model model = findModelFromRepository( artifact, remoteArtifactRepositories, localRepository, allowStubModel );

        return buildInternal( "Artifact [" + artifact + "]", model, localRepository, remoteArtifactRepositories, null, null, false );
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
        return buildStandaloneSuperProject( localRepository, profileManager );
    }
    
    public MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository, ProfileManager profileManager ) 
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        superModel.setGroupId( STANDALONE_SUPERPOM_GROUPID );

        superModel.setArtifactId( STANDALONE_SUPERPOM_ARTIFACTID );

        superModel.setVersion( STANDALONE_SUPERPOM_VERSION );

        MavenProject project = new MavenProject( superModel );
        
        ProjectBuildContext projectContext = ProjectBuildContext.getProjectBuildContext( buildContextManager, true );
        
        projectContext.setCurrentProject( project );
        projectContext.store( buildContextManager );
        
        String projectId = safeVersionlessKey( STANDALONE_SUPERPOM_GROUPID, STANDALONE_SUPERPOM_ARTIFACTID );

        List activeProfiles = profileAdvisor.applyActivatedProfiles( superModel, null, profileManager.getExplicitlyActivatedIds(), profileManager.getExplicitlyDeactivatedIds() );
        List activeExternalProfiles = profileAdvisor.applyActivatedExternalProfiles( superModel, null, profileManager );
        
        LinkedHashSet profiles = new LinkedHashSet();
        
        if ( activeProfiles != null && !activeProfiles.isEmpty() )
        {
            profiles.addAll( activeProfiles );
        }
        
        if ( activeExternalProfiles != null && !activeExternalProfiles.isEmpty() )
        {
            profiles.addAll( activeExternalProfiles );
        }

        project.setActiveProfiles( new ArrayList( profiles ) );

        project.setOriginalModel( superModel );

        try
        {
            project = processProjectLogic( "<Super-POM>", project, null, null, STRICT_MODEL_PARSING );

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

    /**
     * @todo move to metadatasource itself?
     */
    public MavenProject buildWithDependencies( File projectDescriptor,
                                               ArtifactRepository localRepository,
                                               ProfileManager profileManager,
                                               TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        MavenProject project = build( projectDescriptor, localRepository, profileManager );

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

        Map managedVersions = createManagedVersionMap( projectId, project.getDependencyManagement() );

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

    private Map createManagedVersionMap( String projectId, DependencyManagement dependencyManagement )
        throws ProjectBuildingException
    {
        Map map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap();
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
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + d.getVersion() +
                        "' for dependency '" + d.getManagementKey() + "': " + e.getMessage(), e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    private MavenProject buildFromSourceFileInternal( File projectDescriptor,
                                                      ArtifactRepository localRepository,
                                                      ProfileManager profileManager,
                                                      boolean checkDistributionManagementStatus )
        throws ProjectBuildingException
    {
        // TODO: Remove this once we have build-context stuff working...
        if ( !container.getContext().contains( "SystemProperties" ) )
        {
            container.addContextValue("SystemProperties", System.getProperties());
        }
        
        Model model = readModel( "unknown", projectDescriptor, STRICT_MODEL_PARSING );

        MavenProject project = buildInternal( projectDescriptor.getAbsolutePath(),
                                              model,
                                              localRepository,
                                              buildArtifactRepositories( getSuperModel() ),
                                              projectDescriptor,
                                              profileManager,
                                              STRICT_MODEL_PARSING );

        if ( checkDistributionManagementStatus )
        {
            if ( project.getDistributionManagement() != null && project.getDistributionManagement().getStatus() != null )
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
        Artifact projectArtifact;

        // if the artifact is not a POM, we need to construct a POM artifact based on the artifact parameter given.
        if ( "pom".equals( artifact.getType() ) )
        {
            projectArtifact = artifact;
        }
        else
        {
            getLogger().warn( "Attempting to build MavenProject instance for Artifact of type: " + artifact.getType() + "; constructing POM artifact instead." );

            projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                     artifact.getArtifactId(),
                                                                     artifact.getVersion(),
                                                                     artifact.getScope() );
        }

        Model model;

        String projectId = ArtifactUtils.versionlessKey( projectArtifact );

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
            if ( allowStubModel )
            {
                getLogger().debug( "Artifact not found - using stub model: " + e.getMessage() );

                model = createStubModel( projectArtifact );
            }
            else
            {
                throw new ProjectBuildingException( projectId, "POM '" + projectId + "' not found in repository: " + e.getMessage(), e );
            }
        }

        return model;
    }

    private void checkStatusAndUpdate( Artifact projectArtifact,
                                       ArtifactStatus status, File file,
                                       List remoteArtifactRepositories,
                                       ArtifactRepository localRepository )
        throws ArtifactNotFoundException
    {
        // TODO: configurable actions dependant on status
        if ( !projectArtifact.isSnapshot() && status.compareTo( ArtifactStatus.DEPLOYED ) < 0 )
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
                                        ArtifactRepository localRepository,
                                        List parentSearchRepositories,
                                        File projectDescriptor,
                                        ProfileManager externalProfileManager,
                                        boolean strict )
        throws ProjectBuildingException
    {
        File projectDir = null;

        if ( projectDescriptor != null )
        {
            projectDir = projectDescriptor.getAbsoluteFile().getParentFile();
        }

        Model superModel = getSuperModel();

        MavenProject superProject = new MavenProject( superModel );
        
        String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );

        List explicitlyActive;
        List explicitlyInactive;
        
        if ( externalProfileManager != null )
        {
            // used to trigger the caching of SystemProperties in the container context...
            try
            {
                externalProfileManager.getActiveProfiles();
            }
            catch ( ProfileActivationException e )
            {
                throw new ProjectBuildingException( projectId, "Failed to activate external profiles.", e );
            }
            
            explicitlyActive = externalProfileManager.getExplicitlyActivatedIds();
            explicitlyInactive = externalProfileManager.getExplicitlyDeactivatedIds();
        }
        else
        {
            explicitlyActive = Collections.EMPTY_LIST;
            explicitlyInactive = Collections.EMPTY_LIST;
        }
        
        superProject.setActiveProfiles( profileAdvisor.applyActivatedProfiles( superModel, null, explicitlyActive, explicitlyInactive ) );

        //noinspection CollectionDeclaredAsConcreteClass
        LinkedList lineage = new LinkedList();

        LinkedHashSet aggregatedRemoteWagonRepositories = collectInitialRepositories( model, superModel,
                                                                                      parentSearchRepositories,
                                                                                      projectDir, explicitlyActive,
                                                                                      explicitlyInactive );
        
        Model originalModel = ModelUtils.cloneModel( model );

        MavenProject project = null;
        try
        {
            project = assembleLineage( model,
                                       lineage,
                                       localRepository,
                                       projectDir,
                                       parentSearchRepositories,
                                       aggregatedRemoteWagonRepositories,
                                       externalProfileManager,
                                       strict );
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
                getLogger().debug( "Cannot determine whether " + currentProject.getId() + " is a module of " + previousProject.getId() + ". Reason: " + e.getMessage(), e );
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

        try
        {
            project = processProjectLogic( pomLocation, project, externalProfileManager, projectDir, strict );
        }
        catch ( ModelInterpolationException e )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, e.getMessage(), e );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, e.getMessage(), e );
        }

        ProjectBuildCache projectBuildCache = ProjectBuildCache.read( buildContextManager );
        projectBuildCache.cacheProject( project );
        projectBuildCache.store( buildContextManager );

        // jvz:note
        // this only happens if we are building from a source file
        if ( projectDescriptor != null )
        {
            // Only translate the base directory for files in the source tree
            pathTranslator.alignToBaseDirectory( project.getModel(), projectDescriptor.getParentFile() );

            Build build = project.getBuild();

            project.addCompileSourceRoot( build.getSourceDirectory() );

            project.addScriptSourceRoot( build.getScriptSourceDirectory() );

            project.addTestCompileSourceRoot( build.getTestSourceDirectory() );

            // Only track the file of a POM in the source tree
            project.setFile( projectDescriptor );
        }
        
        MavenProject rawParent = project.getParent();
        
        if ( rawParent != null )
        {
            MavenProject processedParent = (MavenProject) projectBuildCache.getCachedProject( rawParent );
            
            // yeah, this null check might be a bit paranoid, but better safe than sorry...
            if ( processedParent != null )
            {
                project.setParent( processedParent );
            }
        }

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
    private LinkedHashSet collectInitialRepositories( Model model, Model superModel, List parentSearchRepositories,
                                                      File projectDir, List explicitlyActive, List explicitlyInactive )
        throws ProjectBuildingException
    {
        LinkedHashSet collected = new LinkedHashSet();
        
        collectInitialRepositoriesFromModel( collected, model, projectDir, explicitlyActive, explicitlyInactive );

        collectInitialRepositoriesFromModel( collected, superModel, projectDir, explicitlyActive, explicitlyInactive );

        if ( parentSearchRepositories != null && !parentSearchRepositories.isEmpty() )
        {
            collected.addAll( parentSearchRepositories );
        }
        
        return collected;
    }

    private void collectInitialRepositoriesFromModel( LinkedHashSet collected, Model model, File projectDir,
                                                      List explicitlyActive, List explicitlyInactive )
        throws ProjectBuildingException
    {
        Set reposFromProfiles = profileAdvisor.getArtifactRepositoriesFromActiveProfiles( model, projectDir,
                                                                                          explicitlyActive,
                                                                                          explicitlyInactive );

        if ( reposFromProfiles != null && !reposFromProfiles.isEmpty() )
        {
            collected.addAll( reposFromProfiles );
        }

        List modelRepos = model.getRepositories();
        if ( modelRepos != null && !modelRepos.isEmpty() )
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

    private String safeVersionlessKey( String groupId, String artifactId )
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
    private MavenProject processProjectLogic( String pomLocation,
                                              MavenProject project,
                                              ProfileManager profileMgr,
                                              File projectDir,
                                              boolean strict )
        throws ProjectBuildingException, ModelInterpolationException, InvalidRepositoryException
    {
        Model model = project.getModel();

        List activeProfiles = project.getActiveProfiles();

        // TODO: Clean this up...we're using this to 'jump' the interpolation step for model properties not expressed in XML.
        //  [BP] - Can this above comment be explained?
        // We don't need all the project methods that are added over those in the model, but we do need basedir
        // mkleint - using System.getProperties() is almost definitely bad for embedding.
        Map context = new HashMap( System.getProperties() );

        if ( projectDir != null )
        {
            context.put( "basedir", projectDir.getAbsolutePath() );
        }

        // TODO: this is a hack to ensure MNG-2124 can be satisfied without triggering MNG-1927
        //  MNG-1927 relies on the false assumption that ${project.build.*} evaluates to null, which occurs before
        //  MNG-2124 is fixed. The null value would leave it uninterpolated, to be handled after path translation.
        //  Until these steps are correctly sequenced, we guarantee these fields remain uninterpolated.
        context.put( "build.directory", null );
        context.put( "build.outputDirectory", null );
        context.put( "build.testOutputDirectory", null );
        context.put( "build.sourceDirectory", null );
        context.put( "build.testSourceDirectory", null );

        model = modelInterpolator.interpolate( model, context, strict );

        // interpolation is before injection, because interpolation is off-limits in the injected variables
        modelDefaultsInjector.injectDefaults( model );

        MavenProject parentProject = project.getParent();

        Model originalModel = project.getOriginalModel();
        
        Artifact parentArtifact = project.getParentArtifact();

        // We will return a different project object using the new model (hence the need to return a project, not just modify the parameter)
        project = new MavenProject( model );

        project.setOriginalModel( originalModel );

        project.setActiveProfiles( activeProfiles );

        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        Artifact projectArtifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                                        project.getVersion(), project.getPackaging() );
        project.setArtifact( projectArtifact );

        project.setPluginArtifactRepositories( mavenTools.buildArtifactRepositories( model.getPluginRepositories() ) );

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
//            Artifact parentArtifact = artifactFactory.createParentArtifact( parentProject.getGroupId(),
//                                                                            parentProject.getArtifactId(),
//                                                                            parentProject.getVersion() );
            // the parent artifact from the parameter passed project instance is resolved.
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
            mavenTools.buildArtifactRepositories( model.getRepositories() ) );

        // TODO: these aren't taking active project artifacts into consideration in the reactor
        project.setPluginArtifacts( createPluginArtifacts( projectId, project.getBuildPlugins() ) );

        project.setReportArtifacts( createReportArtifacts( projectId, project.getReportPlugins() ) );

        project.setExtensionArtifacts( createExtensionArtifacts( projectId, project.getBuildExtensions() ) );

        return project;
    }

    /**
     * @noinspection CollectionDeclaredAsConcreteClass
     * @todo We need to find an effective way to unit test parts of this method!
     * @todo Refactor this into smaller methods with discrete purposes.
     */
    private MavenProject assembleLineage( Model model,
                                          LinkedList lineage,
                                          ArtifactRepository localRepository,
                                          File projectDir,
                                          List parentSearchRepositories,
                                          Set aggregatedRemoteWagonRepositories,
                                          ProfileManager externalProfileManager,
                                          boolean strict )
        throws ProjectBuildingException, InvalidRepositoryException
    {
        ModelLineage modelLineage = new DefaultModelLineage();
        modelLineage.setOrigin( model, new File( projectDir, "pom.xml" ), new ArrayList( aggregatedRemoteWagonRepositories ) );
        
        modelLineageBuilder.resumeBuildingModelLineage( modelLineage, localRepository, externalProfileManager );
        
        ProjectBuildContext projectContext = ProjectBuildContext.getProjectBuildContext( buildContextManager, true );
        
        projectContext.setModelLineage( modelLineage );
        projectContext.store( buildContextManager );
        
        List explicitlyActive;
        List explicitlyInactive;
        
        if ( externalProfileManager != null )
        {
            explicitlyActive = externalProfileManager.getExplicitlyActivatedIds();
            explicitlyInactive = externalProfileManager.getExplicitlyDeactivatedIds();
        }
        else
        {
            explicitlyActive = Collections.EMPTY_LIST;
            explicitlyInactive = Collections.EMPTY_LIST;
        }
        
        MavenProject lastProject = null;
        for ( ModelLineageIterator it = modelLineage.lineageIterator(); it.hasNext(); )
        {
            Model currentModel = (Model) it.next();
            File currentPom = it.getPOMFile();
            
            MavenProject project = new MavenProject( currentModel );
            project.setFile( currentPom );
            
            projectContext.setCurrentProject( project );
            projectContext.store( buildContextManager );

            project.setActiveProfiles( profileAdvisor.applyActivatedProfiles( currentModel, projectDir, explicitlyActive,
                                                                              explicitlyInactive ) );
            
            if ( lastProject != null )
            {
                lastProject.setParent( project );
                
                lastProject.setParentArtifact( artifactFactory.createParentArtifact( project.getGroupId(), project
                    .getArtifactId(), project.getVersion() ) );
            }
            
            lineage.addFirst( project );
            
            lastProject = project;
        }

        MavenProject result = (MavenProject) lineage.getLast();
        
        if ( externalProfileManager != null )
        {
            LinkedHashSet active = new LinkedHashSet();
            
            List existingActiveProfiles = result.getActiveProfiles();
            if ( existingActiveProfiles != null && !existingActiveProfiles.isEmpty() )
            {
                active.addAll( existingActiveProfiles );
            }
            
            profileAdvisor.applyActivatedExternalProfiles( result.getModel(), projectDir, externalProfileManager );
        }
        
        return result;
    }

    private Model readModel( String projectId, File file, boolean strict )
        throws ProjectBuildingException
    {
        Reader reader = null;
        try
        {
            reader = new FileReader( file );
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

    private Model readModel( String projectId, String pomLocation, Reader reader, boolean strict )
        throws IOException, InvalidProjectModelException
    {
        StringWriter sw = new StringWriter();

        IOUtil.copy( reader, sw );

        String modelSource = sw.toString();

        if ( modelSource.indexOf( "<modelVersion>4.0.0" ) < 0 )
        {
            throw new InvalidProjectModelException( projectId, pomLocation, "Not a v4.0.0 POM." );
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

    private Model readModel( String projectId, URL url, boolean strict )
        throws ProjectBuildingException
    {
        InputStreamReader reader = null;
        try
        {
            reader = new InputStreamReader( url.openStream() );
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

    private static String createCacheKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    protected Set createPluginArtifacts( String projectId, List plugins )
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
    protected Set createReportArtifacts( String projectId, List reports )
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
    protected Set createExtensionArtifacts( String projectId, List extensions )
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

        return readModel( projectId, url, STRICT_MODEL_PARSING );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
