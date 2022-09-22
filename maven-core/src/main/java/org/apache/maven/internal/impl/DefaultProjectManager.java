package org.apache.maven.internal.impl;

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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.ResolutionScope;
import org.apache.maven.api.Scope;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class DefaultProjectManager implements ProjectManager
{

    private final Session session;
    private final ArtifactManager artifactManager;
    private final PlexusContainer container;

    public DefaultProjectManager( Session session,
                                  ArtifactManager artifactManager,
                                  PlexusContainer container )
    {
        this.session = session;
        this.artifactManager = artifactManager;
        this.container = container;
    }

    @Nonnull
    @Override
    public Optional<Path> getPath( Project project )
    {
        // TODO: apiv4
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    @Nonnull
    @Override
    public Collection<Artifact> getAttachedArtifacts( Project project )
    {
        AbstractSession session = ( (DefaultProject ) project ).getSession();
        Collection<Artifact> attached = getMavenProject( project ).getAttachedArtifacts().stream()
                .map( RepositoryUtils::toArtifact )
                .map( session::getArtifact )
                .collect( Collectors.toList() );
        return Collections.unmodifiableCollection( attached );
    }

    @Override
    public void attachArtifact( Project project, Artifact artifact, Path path )
    {
        getMavenProject( project ).addAttachedArtifact(
                RepositoryUtils.toArtifact( ( ( DefaultProject ) project ).getSession().toArtifact( artifact ) ) );
        artifactManager.setPath( artifact, path );
    }

    @Override
    public List<String> getCompileSourceRoots( Project project )
    {
        List<String> roots = getMavenProject( project ).getCompileSourceRoots();
        return Collections.unmodifiableList( roots );
    }

    @Override
    public void addCompileSourceRoot( Project project, String sourceRoot )
    {
        List<String> roots = getMavenProject( project ).getCompileSourceRoots();
        roots.add( sourceRoot );
    }

    @Override
    public List<String> getTestCompileSourceRoots( Project project )
    {
        List<String> roots = getMavenProject( project ).getTestCompileSourceRoots();
        return Collections.unmodifiableList( roots );
    }

    @Override
    public void addTestCompileSourceRoot( Project project, String sourceRoot )
    {
        List<String> roots = getMavenProject( project ).getTestCompileSourceRoots();
        roots.add( sourceRoot );
    }

    @Override
    public List<RemoteRepository> getRepositories( Project project )
    {
        // TODO: apiv4
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    @Override
    public List<Artifact> getResolvedDependencies( Project project, ResolutionScope scope )
    {
        Collection<String> toResolve = toScopes( scope );
        try
        {
            LifecycleDependencyResolver lifecycleDependencyResolver =
                    container.lookup( LifecycleDependencyResolver.class );
            Set<org.apache.maven.artifact.Artifact> artifacts = lifecycleDependencyResolver.resolveProjectArtifacts(
                    getMavenProject( project ),
                    toResolve,
                    toResolve,
                    ( ( DefaultSession ) session ).getMavenSession(),
                    false,
                    Collections.emptySet()
            );
            return artifacts.stream()
                .map( RepositoryUtils::toArtifact )
                .map( ( ( DefaultSession ) session )::getArtifact )
                .collect( Collectors.toList() );
        }
        catch ( LifecycleExecutionException | ComponentLookupException e )
        {
            throw new MavenException( "Unable to resolve project dependencies", e );
        }
    }

    @Override
    public Node getCollectedDependencies( Project project, ResolutionScope scope )
    {
        // TODO: apiv4
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    private MavenProject getMavenProject( Project project )
    {
        return ( ( DefaultProject ) project ).getProject();
    }

    private Collection<String> toScopes( ResolutionScope scope )
    {
        return scope.scopes().stream().map( Scope::id ).collect( Collectors.toList() );
    }

}
