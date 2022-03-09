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

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectManager;

public class DefaultProjectManager implements ProjectManager
{

    private final ArtifactManager artifactManager;

    public DefaultProjectManager( ArtifactManager artifactManager )
    {
        this.artifactManager = artifactManager;
    }

    @Nonnull
    @Override
    public Optional<Path> getPath( Project project )
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Collection<Artifact> getAttachedArtifacts( Project project )
    {
        DefaultSession session = ( (DefaultProject ) project ).getSession();
        Collection<Artifact> attached = ( ( DefaultProject ) project ).getProject().getAttachedArtifacts().stream()
                .map( RepositoryUtils::toArtifact )
                .map( session::getArtifact )
                .collect( Collectors.toList() );
        return Collections.unmodifiableCollection( attached );
    }

    @Override
    public void attachArtifact( Project project, Artifact artifact, Path path )
    {
        ( ( DefaultProject ) project ).getProject().addAttachedArtifact(
                RepositoryUtils.toArtifact( ( ( DefaultProject ) project ).getSession().toArtifact( artifact ) ) );
        artifactManager.setPath( artifact, path );
    }

    @Override
    public List<String> getCompileSourceRoots( Project project )
    {
        List<String> roots = ( ( DefaultProject ) project ).getProject().getCompileSourceRoots();
        return Collections.unmodifiableList( roots );
    }

    @Override
    public void addCompileSourceRoot( Project project, String sourceRoot )
    {
        List<String> roots = ( ( DefaultProject ) project ).getProject().getCompileSourceRoots();
        roots.add( sourceRoot );
    }

    @Override
    public List<String> getTestCompileSourceRoots( Project project )
    {
        List<String> roots = ( ( DefaultProject ) project ).getProject().getTestCompileSourceRoots();
        return Collections.unmodifiableList( roots );
    }

    @Override
    public void addTestCompileSourceRoot( Project project, String sourceRoot )
    {
        List<String> roots = ( ( DefaultProject ) project ).getProject().getTestCompileSourceRoots();
        roots.add( sourceRoot );
    }

    @Override
    public List<RemoteRepository> getRepositories( Project project )
    {
        return null;
    }

    @Override
    public List<Artifact> getResolvedDependencies( Project project, ResolutionScope scope )
    {
        return null;
    }

    @Override
    public Node getCollectedDependencies( Project project, ResolutionScope scope )
    {
        return null;
    }
}
