package org.apache.maven.project;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.activation.ProfileActivationCalculator;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @version $Id: DefaultMavenProjectBuilder.java,v 1.37 2005/03/08 01:55:22
 *          trygvis Exp $
 */
public class DefaultMavenProjectBuilder
    extends AbstractLogEnabled
    implements MavenProjectBuilder, Initializable, Contextualizable
{
    // TODO: remove
    private PlexusContainer container;

    private ArtifactResolver artifactResolver;

    private ArtifactFactory artifactFactory;

    private ModelInheritanceAssembler modelInheritanceAssembler;

    private ModelValidator validator;

    // TODO: make it a component
    private MavenXpp3Reader modelReader;

    private PathTranslator pathTranslator;

    private ModelDefaultsInjector modelDefaultsInjector;

    private ModelInterpolator modelInterpolator;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private ProfileActivationCalculator profileActivationCalculator;

    private final Map modelCache = new HashMap();

    public static final String MAVEN_MODEL_VERSION = "4.0.0";

    public void initialize()
    {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    /**
     * @todo move to metadatasource itself?
     */
    public MavenProject buildWithDependencies( File projectDescriptor, ArtifactRepository localRepository,
                                               List externalProfiles )
        throws ProjectBuildingException, ArtifactResolutionException
    {
        ArtifactMetadataSource source;
        try
        {
            source = (ArtifactMetadataSource) container.lookup( ArtifactMetadataSource.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new ProjectBuildingException( "Unable to get the artifact metadata source component", e );
        }
        return buildWithDependencies( projectDescriptor, localRepository, source, externalProfiles );
    }

    public MavenProject buildWithDependencies( File projectDescriptor, ArtifactRepository localRepository,
                                               ArtifactMetadataSource artifactMetadataSource, List externalProfiles )
        throws ProjectBuildingException, ArtifactResolutionException
    {
        MavenProject project = buildFromSourceFile( projectDescriptor, localRepository, externalProfiles );

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

        Map managedVersions = createManagedVersionMap( project.getDependencyManagement() );
        ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getDependencyArtifacts(),
                                                                                projectArtifact, managedVersions,
                                                                                localRepository,
                                                                                project.getRemoteArtifactRepositories(),
                                                                                artifactMetadataSource );

        project.setArtifacts( result.getArtifacts() );
        return project;
    }

    private Map createManagedVersionMap( DependencyManagement dependencyManagement )
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
                                                                                  d.getScope() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new ProjectBuildingException( "Unable to parse dependency version", e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository, List externalProfiles )
        throws ProjectBuildingException
    {
        return buildFromSourceFile( projectDescriptor, localRepository, externalProfiles );
    }

    private MavenProject buildFromSourceFile( File projectDescriptor, ArtifactRepository localRepository,
                                              List externalProfiles )
        throws ProjectBuildingException
    {
        Model model = readModel( projectDescriptor );

        // Always cache files in the source tree over those in the repository
        modelCache.put( createCacheKey( model.getGroupId(), model.getArtifactId(), model.getVersion() ), model );

        MavenProject project = build( projectDescriptor.getAbsolutePath(), model, localRepository,
                                      Collections.EMPTY_LIST, externalProfiles );

        // Only translate the base directory for files in the source tree
        pathTranslator.alignToBaseDirectory( project.getModel(), projectDescriptor );

        Build build = project.getBuild();
        project.addCompileSourceRoot( build.getSourceDirectory() );
        project.addScriptSourceRoot( build.getScriptSourceDirectory() );
        project.addTestCompileSourceRoot( build.getTestSourceDirectory() );

        // Only track the file of a POM in the source tree
        project.setFile( projectDescriptor );

        return project;
    }

    public MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                             ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        Model model = findModelFromRepository( artifact, remoteArtifactRepositories, localRepository );

        return build( "Artifact [" + artifact.getId() + "]", model, localRepository, remoteArtifactRepositories,
                      Collections.EMPTY_LIST );
    }

    private Model findModelFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                           ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        Model model = getCachedModel( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        if ( model == null )
        {
            // TODO: can't assume artifact is a POM
            try
            {
                artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                // TODO: a not found would be better vs other errors
                throw new ProjectBuildingException( "Unable to find the POM in the repository", e );
            }

//                String path = localRepository.pathOfMetadata( new ProjectArtifactMetadata( artifact, null ) );
//                File file = new File( localRepository.getBasedir(), path );
            File file = artifact.getFile();
            model = readModel( file );
        }

        // TODO: this is gross. Would like to give it the whole model, but maven-artifact shouldn't depend on that
        // Can a maven-core implementation of the Artifact interface store it, and be used in the exceptions?
        String downloadUrl = null;
        if ( model.getDistributionManagement() != null )
        {
            downloadUrl = model.getDistributionManagement().getDownloadUrl();
        }
        if ( downloadUrl != null )
        {
            artifact.setDownloadUrl( downloadUrl );
        }
        else
        {
            artifact.setDownloadUrl( model.getUrl() );
        }

        return model;
    }

    private MavenProject build( String pomLocation, Model model, ArtifactRepository localRepository,
                                List remoteArtifactRepositories, List externalProfiles )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        LinkedList lineage = new LinkedList();

        Set aggregatedRemoteWagonRepositories = new HashSet();
        if ( remoteArtifactRepositories != null && !remoteArtifactRepositories.isEmpty() )
        {
            aggregatedRemoteWagonRepositories.addAll( remoteArtifactRepositories );
        }

        aggregatedRemoteWagonRepositories.addAll( ProjectUtils.buildArtifactRepositories( superModel.getRepositories(),
                                                                                          artifactRepositoryFactory,
                                                                                          container ) );

        for ( Iterator i = externalProfiles.iterator(); i.hasNext(); )
        {
            Profile externalProfile = (Profile) i.next();

            for ( Iterator repoIterator = externalProfile.getRepositories().iterator(); repoIterator.hasNext(); )
            {
                Repository mavenRepo = (Repository) repoIterator.next();

                ArtifactRepository artifactRepo = ProjectUtils.buildArtifactRepository( mavenRepo,
                                                                                        artifactRepositoryFactory,
                                                                                        container );

                aggregatedRemoteWagonRepositories.add( artifactRepo );
            }
        }

        List repositories = new ArrayList( aggregatedRemoteWagonRepositories );

        MavenProject project = assembleLineage( model, lineage, repositories, localRepository );

        // we don't have to force the collision exception for superModel here, it's already been done in getSuperModel()
        Model previous = superModel;

        for ( Iterator i = lineage.iterator(); i.hasNext(); )
        {
            MavenProject currentProject = (MavenProject) i.next();

            Model current = currentProject.getModel();

            modelInheritanceAssembler.assembleModelInheritance( current, previous );

            previous = current;
        }

        try
        {
            project = processProjectLogic( pomLocation, project, repositories, externalProfiles );
        }
        catch ( ModelInterpolationException e )
        {
            throw new ProjectBuildingException( "Error building project from \'" + pomLocation + "\': " + model.getId(),
                                                e );
        }
        return project;
    }

    /**
     * @todo can this take in a model instead of a project and still be successful?
     * @todo In fact, does project REALLY need a MavenProject as a parent? Couldn't it have just a wrapper around a
     * model that supported parents which were also the wrapper so that inheritence was assembled. We don't really need
     * the resolved source roots, etc for the parent - that occurs for the parent when it is constructed independently
     * and projects are not cached or reused
     */
    private MavenProject processProjectLogic( String pomLocation, MavenProject project, List remoteRepositories,
                                              List externalProfiles )
        throws ProjectBuildingException, ModelInterpolationException
    {
        Model model = project.getModel();
        String key = createCacheKey( model.getGroupId(), model.getArtifactId(), model.getVersion() );
        Model cachedModel = (Model) modelCache.get( key );
        if ( cachedModel == null )
        {
            modelCache.put( key, model );
        }

        List activeProfiles = new ArrayList( externalProfiles );

        List activePomProfiles = profileActivationCalculator.calculateActiveProfiles( model.getProfiles() );

        activeProfiles.addAll( activePomProfiles );

        Properties profileProperties = new Properties();

        for ( Iterator it = activeProfiles.iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();

            modelInheritanceAssembler.mergeProfileWithModel( model, profile );

            profileProperties.putAll( profile.getProperties() );
        }

        // TODO: Clean this up...we're using this to 'jump' the interpolation step for model properties not expressed in XML.

        model = modelInterpolator.interpolate( model );

        // interpolation is before injection, because interpolation is off-limits in the injected variables
        modelDefaultsInjector.injectDefaults( model );

        MavenProject parentProject = project.getParent();

        project = new MavenProject( model );

        project.addProfileProperties( profileProperties );

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
            project.setDistributionManagementArtifactRepository(
                ProjectUtils.buildArtifactRepository( dm.getRepository(), artifactRepositoryFactory, container ) );
        }

        project.setParent( parentProject );

        if ( parentProject != null )
        {
            Artifact parentArtifact = artifactFactory.createProjectArtifact( parentProject.getGroupId(),
                                                                             parentProject.getArtifactId(),
                                                                             parentProject.getVersion() );
            project.setParentArtifact( parentArtifact );
        }

        project.setRemoteArtifactRepositories( remoteRepositories );
        project.setDependencyArtifacts( createArtifacts( project.getDependencies() ) );
        project.setPluginArtifacts( createPluginArtifacts( project.getBuildPlugins() ) );

        ModelValidationResult validationResult = validator.validate( model );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new ProjectBuildingException( "Failed to validate POM for \'" + pomLocation +
                "\'.\n\n  Reason(s):\n" + validationResult.render( "  " ) );
        }

        return project;
    }

    private MavenProject assembleLineage( Model model, LinkedList lineage, List aggregatedRemoteWagonRepositories,
                                          ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        if ( !model.getRepositories().isEmpty() )
        {
            List respositories = ProjectUtils.buildArtifactRepositories( model.getRepositories(),
                                                                         artifactRepositoryFactory, container );

            for ( Iterator it = respositories.iterator(); it.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) it.next();

                if ( !aggregatedRemoteWagonRepositories.contains( repository ) )
                {
                    aggregatedRemoteWagonRepositories.add( repository );
                }
            }
        }

        MavenProject project = new MavenProject( model );

        lineage.addFirst( project );

        Parent parentModel = model.getParent();

        if ( parentModel != null )
        {
            if ( StringUtils.isEmpty( parentModel.getGroupId() ) )
            {
                throw new ProjectBuildingException( "Missing groupId element from parent element" );
            }
            else if ( StringUtils.isEmpty( parentModel.getArtifactId() ) )
            {
                throw new ProjectBuildingException( "Missing artifactId element from parent element" );
            }
            else if ( StringUtils.isEmpty( parentModel.getVersion() ) )
            {
                throw new ProjectBuildingException( "Missing version element from parent element" );
            }

            //!! (**)
            // ----------------------------------------------------------------------
            // Do we have the necessary information to actually find the parent
            // POMs here?? I don't think so ... Say only one remote repository is
            // specified and that is ibiblio then this model that we just read doesn't
            // have any repository information ... I think we might have to inherit
            // as we go in order to do this.
            // ----------------------------------------------------------------------

            Artifact artifact = artifactFactory.createParentArtifact( parentModel.getGroupId(),
                                                                      parentModel.getArtifactId(),
                                                                      parentModel.getVersion() );

            model = findModelFromRepository( artifact, aggregatedRemoteWagonRepositories, localRepository );

            MavenProject parent = assembleLineage( model, lineage, aggregatedRemoteWagonRepositories, localRepository );

            project.setParent( parent );

            project.setParentArtifact( artifact );
        }

        return project;
    }

    private Model readModel( File file )
        throws ProjectBuildingException
    {
        FileReader reader = null;
        try
        {
            reader = new FileReader( file );
            return modelReader.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new ProjectBuildingException( "Could not find the model file '" + file.getAbsolutePath() + "'.", e );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "Failed to build model from file '" + file.getAbsolutePath() +
                "'.\nError: \'" + e.getLocalizedMessage() + "\'", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ProjectBuildingException( "Failed to parse model from file '" + file.getAbsolutePath() +
                "'.\nError: \'" + e.getLocalizedMessage() + "\'", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private Model readModel( URL url )
        throws ProjectBuildingException
    {
        InputStreamReader reader = null;
        try
        {
            reader = new InputStreamReader( url.openStream() );
            return modelReader.read( reader );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "Failed build model from URL \'" + url.toExternalForm() +
                "\'\nError: \'" + e.getLocalizedMessage() + "\'", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ProjectBuildingException( "Failed to parse model from URL \'" + url.toExternalForm() +
                "\'\nError: \'" + e.getLocalizedMessage() + "\'", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private Model getCachedModel( String groupId, String artifactId, String version )
    {
        return (Model) modelCache.get( createCacheKey( groupId, artifactId, version ) );
    }

    private static String createCacheKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    protected Set createArtifacts( List dependencies )
        throws ProjectBuildingException
    {
        try
        {
            return MavenMetadataSource.createArtifacts( artifactFactory, dependencies, null, null );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new ProjectBuildingException( "Unable to parse dependency version", e );
        }
    }

    protected Set createPluginArtifacts( List plugins )
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

            Artifact artifact = null;
            try
            {
                artifact = artifactFactory.createPluginArtifact( p.getGroupId(), p.getArtifactId(),
                                                                 VersionRange.createFromVersionSpec( version ) );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new ProjectBuildingException( "Unable to parse plugin version", e );
            }

            if ( artifact != null )
            {
                pluginArtifacts.add( artifact );
            }
        }

        return pluginArtifacts;
    }

    public MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository, List externalProfiles )
        throws ProjectBuildingException
    {
        Model superModel = getSuperModel();

        superModel.setGroupId( STANDALONE_SUPERPOM_GROUPID );

        superModel.setArtifactId( STANDALONE_SUPERPOM_ARTIFACTID );

        superModel.setVersion( STANDALONE_SUPERPOM_VERSION );

        MavenProject project = new MavenProject( superModel );

        try
        {
            project.setFile( new File( ".", "pom.xml" ) );

            List remoteRepositories = ProjectUtils.buildArtifactRepositories( superModel.getRepositories(),
                                                                              artifactRepositoryFactory, container );

            project = processProjectLogic( "<Super-POM>", project, remoteRepositories, externalProfiles );

            return project;
        }
        catch ( ModelInterpolationException e )
        {
            throw new ProjectBuildingException( "Error building super-project", e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Model getSuperModel()
        throws ProjectBuildingException
    {
        URL url = DefaultMavenProjectBuilder.class.getResource( "pom-" + MAVEN_MODEL_VERSION + ".xml" );

        return readModel( url );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
