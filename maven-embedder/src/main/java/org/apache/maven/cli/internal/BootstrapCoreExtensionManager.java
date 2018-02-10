package org.apache.maven.cli.internal;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.internal.DefaultPluginDependenciesResolver;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

/**
 * BootstrapCoreExtensionManager
 */
@Named
public class BootstrapCoreExtensionManager
{
    private final Logger log;

    private final DefaultPluginDependenciesResolver pluginDependenciesResolver;

    private final DefaultRepositorySystemSessionFactory repositorySystemSessionFactory;

    private final ClassWorld classWorld;

    private final ClassRealm parentRealm;

    @Inject
    public BootstrapCoreExtensionManager( Logger log, DefaultPluginDependenciesResolver pluginDependenciesResolver,
                                          DefaultRepositorySystemSessionFactory repositorySystemSessionFactory,
                                          PlexusContainer container )
    {
        this.log = log;
        this.pluginDependenciesResolver = pluginDependenciesResolver;
        this.repositorySystemSessionFactory = repositorySystemSessionFactory;
        this.classWorld = ( (DefaultPlexusContainer) container ).getClassWorld();
        this.parentRealm = container.getContainerRealm();
    }

    public List<CoreExtensionEntry> loadCoreExtensions( MavenExecutionRequest request, Set<String> providedArtifacts,
                                                        List<CoreExtension> extensions )
        throws Exception
    {
        RepositorySystemSession repoSession = repositorySystemSessionFactory.newRepositorySession( request );
        List<RemoteRepository> repositories = RepositoryUtils.toRepos( request.getPluginArtifactRepositories() );

        return resolveCoreExtensions( repoSession, repositories, providedArtifacts, extensions );
    }

    private List<CoreExtensionEntry> resolveCoreExtensions( RepositorySystemSession repoSession,
                                                            List<RemoteRepository> repositories,
                                                            Set<String> providedArtifacts,
                                                            List<CoreExtension> configuration )
        throws Exception
    {
        List<CoreExtensionEntry> extensions = new ArrayList<>();

        DependencyFilter dependencyFilter = new ExclusionsDependencyFilter( providedArtifacts );

        for ( CoreExtension extension : configuration )
        {
            List<Artifact> artifacts = resolveExtension( extension, repoSession, repositories, dependencyFilter );
            if ( !artifacts.isEmpty() )
            {
                extensions.add( createExtension( extension, artifacts ) );
            }
        }

        return Collections.unmodifiableList( extensions );
    }

    private CoreExtensionEntry createExtension( CoreExtension extension, List<Artifact> artifacts )
        throws Exception
    {
        String realmId =
            "coreExtension>" + extension.getGroupId() + ":" + extension.getArtifactId() + ":" + extension.getVersion();
        ClassRealm realm = classWorld.newRealm( realmId, null );
        log.debug( "Populating class realm " + realm.getId() );
        realm.setParentRealm( parentRealm );
        for ( Artifact artifact : artifacts )
        {
            File file = artifact.getFile();
            log.debug( "  Included " + file );
            realm.addURL( file.toURI().toURL() );
        }
        return CoreExtensionEntry.discoverFrom( realm, Collections.singleton( artifacts.get( 0 ).getFile() ) );
    }

    private List<Artifact> resolveExtension( CoreExtension extension, RepositorySystemSession repoSession,
                                             List<RemoteRepository> repositories, DependencyFilter dependencyFilter )
        throws PluginResolutionException
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId( extension.getGroupId() );
        plugin.setArtifactId( extension.getArtifactId() );
        plugin.setVersion( extension.getVersion() );

        DependencyNode root =
            pluginDependenciesResolver.resolveCoreExtension( plugin, dependencyFilter, repositories, repoSession );
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        root.accept( nlg );
        List<Artifact> artifacts = nlg.getArtifacts( false );

        return artifacts;
    }
}
