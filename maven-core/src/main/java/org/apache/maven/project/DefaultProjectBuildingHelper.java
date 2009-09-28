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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Assists the project builder. <strong>Warning:</strong> This is an internal utility class that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ProjectBuildingHelper.class )
public class DefaultProjectBuildingHelper
    implements ProjectBuildingHelper
{

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private ClassRealmManager classRealmManager;

    @Requirement
    private ExtensionRealmCache extensionRealmCache;

    @Requirement
    private ProjectRealmCache projectRealmCache;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private ArtifactFilterManager artifactFilterManager;

    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    private ExtensionDescriptorBuilder extensionDescriptorBuilder = new ExtensionDescriptorBuilder();

    public List<ArtifactRepository> createArtifactRepositories( List<Repository> pomRepositories,
                                                                List<ArtifactRepository> externalRepositories,
                                                                ProjectBuildingRequest request )
        throws InvalidRepositoryException
    {
        List<ArtifactRepository> artifactRepositories = new ArrayList<ArtifactRepository>();

        for ( Repository repository : pomRepositories )
        {
            artifactRepositories.add( repositorySystem.buildArtifactRepository( repository ) );
        }

        repositorySystem.injectMirror( artifactRepositories, request.getMirrors() );

        repositorySystem.injectProxy( artifactRepositories, request.getProxies() );

        repositorySystem.injectAuthentication( artifactRepositories, request.getServers() );

        if ( externalRepositories != null )
        {
            artifactRepositories.addAll( externalRepositories );
        }

        artifactRepositories = repositorySystem.getEffectiveRepositories( artifactRepositories );

        return artifactRepositories;
    }

    public synchronized ProjectRealmCache.CacheRecord createProjectRealm( Model model,
                                                                          RepositoryRequest repositoryRequest )
        throws ArtifactResolutionException, PluginVersionResolutionException
    {
        ClassRealm projectRealm = null;

        List<Plugin> extensionPlugins = new ArrayList<Plugin>();

        Build build = model.getBuild();

        if ( build != null )
        {
            for ( Extension extension : build.getExtensions() )
            {
                Plugin plugin = new Plugin();
                plugin.setGroupId( extension.getGroupId() );
                plugin.setArtifactId( extension.getArtifactId() );
                plugin.setVersion( extension.getVersion() );
                extensionPlugins.add( plugin );
            }

            for ( Plugin plugin : build.getPlugins() )
            {
                if ( plugin.isExtensions() )
                {
                    extensionPlugins.add( plugin );
                }
            }
        }

        if ( extensionPlugins.isEmpty() )
        {
            return new ProjectRealmCache.CacheRecord( null, null );
        }

        List<ClassRealm> extensionRealms = new ArrayList<ClassRealm>();

        Map<ClassRealm, List<String>> exportedPackages = new HashMap<ClassRealm, List<String>>();

        Map<ClassRealm, List<String>> exportedArtifacts = new HashMap<ClassRealm, List<String>>();

        List<Artifact> publicArtifacts = new ArrayList<Artifact>();

        for ( Plugin plugin : extensionPlugins )
        {
            if ( plugin.getVersion() == null )
            {
                PluginVersionRequest versionRequest = new DefaultPluginVersionRequest( plugin, repositoryRequest );
                plugin.setVersion( pluginVersionResolver.resolve( versionRequest ).getVersion() );
            }

            ClassRealm extensionRealm;
            List<Artifact> artifacts;
            ExtensionDescriptor extensionDescriptor = null;

            ExtensionRealmCache.CacheRecord record = extensionRealmCache.get( plugin, repositoryRequest );

            if ( record != null )
            {
                extensionRealm = record.realm;
                artifacts = record.artifacts;
                extensionDescriptor = record.desciptor;
            }
            else
            {
                artifacts = resolveExtensionArtifacts( plugin, repositoryRequest );

                extensionRealm = classRealmManager.createExtensionRealm( plugin );

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Populating extension realm for " + plugin.getId() );
                }

                for ( Artifact artifact : artifacts )
                {
                    if ( artifact.getFile() != null )
                    {
                        if ( logger.isDebugEnabled() )
                        {
                            logger.debug( "  Included: " + artifact.getId() );
                        }

                        try
                        {
                            extensionRealm.addURL( artifact.getFile().toURI().toURL() );
                        }
                        catch ( MalformedURLException e )
                        {
                            // Not going to happen
                        }
                    }
                    else
                    {
                        if ( logger.isDebugEnabled() )
                        {
                            logger.debug( "  Excluded: " + artifact.getId() );
                        }
                    }
                }

                try
                {
                    container.discoverComponents( extensionRealm );
                }
                catch ( Exception e )
                {
                    throw new IllegalStateException( "Failed to discover components in extension realm "
                        + extensionRealm.getId(), e );
                }

                Artifact extensionArtifact = artifacts.get( 0 );
                try
                {
                    extensionDescriptor = extensionDescriptorBuilder.build( extensionArtifact.getFile() );
                }
                catch ( IOException e )
                {
                    String message = "Invalid extension descriptor for " + plugin.getId() + ": " + e.getMessage();
                    if ( logger.isDebugEnabled() )
                    {
                        logger.error( message, e );
                    }
                    else
                    {
                        logger.error( message );
                    }
                }

                extensionRealmCache.put( plugin, repositoryRequest, extensionRealm, artifacts, extensionDescriptor );
            }

            extensionRealms.add( extensionRealm );
            if ( extensionDescriptor != null )
            {
                exportedPackages.put( extensionRealm, extensionDescriptor.getExportedPackages() );
                exportedArtifacts.put( extensionRealm, extensionDescriptor.getExportedArtifacts() );
            }

            if ( !plugin.isExtensions() && artifacts.size() == 1 && artifacts.get( 0 ).getFile() != null )
            {
                /*
                 * This is purely for backward-compat with 2.x where <extensions> consisting of a single artifact where
                 * loaded into the core and hence available to plugins, in contrast to bigger extensions that were
                 * loaded into a dedicated realm which is invisible to plugins.
                 */
                publicArtifacts.addAll( artifacts );
            }
        }

        ProjectRealmCache.CacheRecord record = projectRealmCache.get( extensionRealms );

        if ( record == null )
        {
            projectRealm = classRealmManager.createProjectRealm( model );

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Populating project realm for " + model.getId() );
            }

            for ( Artifact publicArtifact : publicArtifacts )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "  Included: " + publicArtifact.getId() );
                }

                try
                {
                    projectRealm.addURL( publicArtifact.getFile().toURI().toURL() );
                }
                catch ( MalformedURLException e )
                {
                    // can't happen
                }
            }

            Set<String> exclusions = new LinkedHashSet<String>();

            for ( ClassRealm extensionRealm : extensionRealms )
            {
                List<String> excludes = exportedArtifacts.get( extensionRealm );

                if ( excludes != null )
                {
                    exclusions.addAll( excludes );
                }

                List<String> exports = exportedPackages.get( extensionRealm );

                if ( exports == null || exports.isEmpty() )
                {
                    /*
                     * Most existing extensions don't define exported packages, i.e. no classes are to be exposed to
                     * plugins, yet the components provided by the extension (e.g. artifact handlers) must be
                     * accessible, i.e. we still must import the extension realm into the project realm.
                     */
                    exports = Arrays.asList( extensionRealm.getId() );
                }

                for ( String export : exports )
                {
                    projectRealm.importFrom( extensionRealm, export );
                }
            }

            ArtifactFilter extensionArtifactFilter = null;
            if ( !exclusions.isEmpty() )
            {
                extensionArtifactFilter = new ExclusionSetFilter( exclusions );
            }

            projectRealmCache.put( extensionRealms, projectRealm, extensionArtifactFilter );

            record = new ProjectRealmCache.CacheRecord( projectRealm, extensionArtifactFilter );
        }

        return record;
    }

    private List<Artifact> resolveExtensionArtifacts( Plugin extensionPlugin, RepositoryRequest repositoryRequest )
        throws ArtifactResolutionException
    {
        Artifact extensionArtifact = repositorySystem.createPluginArtifact( extensionPlugin );

        Set<Artifact> overrideArtifacts = new LinkedHashSet<Artifact>();
        for ( Dependency dependency : extensionPlugin.getDependencies() )
        {
            overrideArtifacts.add( repositorySystem.createDependencyArtifact( dependency ) );
        }

        ArtifactFilter collectionFilter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM );

        ArtifactFilter resolutionFilter = artifactFilterManager.getCoreArtifactFilter();

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( repositoryRequest );
        request.setArtifact( extensionArtifact );
        request.setArtifactDependencies( overrideArtifacts );
        request.setCollectionFilter( collectionFilter );
        request.setResolutionFilter( resolutionFilter );
        request.setResolveRoot( true );
        request.setResolveTransitively( true );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        resolutionErrorHandler.throwErrors( request, result );

        List<Artifact> extensionArtifacts = new ArrayList<Artifact>( result.getArtifacts() );

        return extensionArtifacts;
    }

}
