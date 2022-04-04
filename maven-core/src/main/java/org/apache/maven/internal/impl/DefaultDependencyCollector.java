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

import org.apache.maven.api.annotations.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;

import org.apache.maven.api.Node;
import org.apache.maven.api.services.DependencyCollector;
import org.apache.maven.api.services.DependencyCollectorException;
import org.apache.maven.api.services.DependencyCollectorRequest;
import org.apache.maven.api.services.DependencyCollectorResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
public class DefaultDependencyCollector implements DependencyCollector
{

    private final RepositorySystem repositorySystem;

    @Inject
    DefaultDependencyCollector( @Nonnull RepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }

    @Nonnull
    @Override
    public DependencyCollectorResult collect( @Nonnull DependencyCollectorRequest request )
            throws DependencyCollectorException, IllegalArgumentException
    {
        nonNull( request, "request can not be null" );
        DefaultSession session = cast( DefaultSession.class, request.getSession(),
                "request.session should be a " + DefaultSession.class );

        Artifact rootArtifact = request.getRootArtifact().map( session::toArtifact ).orElse( null );
        Dependency root = request.getRoot().map( session::toDependency ).orElse( null );
        CollectRequest collectRequest = new CollectRequest()
                .setRootArtifact( rootArtifact )
                .setRoot( root != null ? root : new Dependency( rootArtifact, null ) )
                .setDependencies( session.toDependencies( request.getDependencies() ) )
                .setManagedDependencies( session.toDependencies( request.getManagedDependencies() ) )
                .setRepositories( session.toRepositories( session.getRemoteRepositories() ) );

        try
        {
            final CollectResult
                    result = repositorySystem.collectDependencies( session.getSession(), collectRequest );
            return new DependencyCollectorResult()
            {
                @Override
                public List<Exception> getExceptions()
                {
                    return result.getExceptions();
                }

                @Override
                public Node getRoot()
                {
                    return session.getNode( result.getRoot() );
                }
            };
        }
        catch ( DependencyCollectionException e )
        {
            throw new DependencyCollectorException( "Unable to collect dependencies", e );
        }
    }

}
