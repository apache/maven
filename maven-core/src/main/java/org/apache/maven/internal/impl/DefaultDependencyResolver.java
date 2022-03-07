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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.api.Node;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

@Named
public class DefaultDependencyResolver implements DependencyResolver
{

    private final RepositorySystem repositorySystem;

    @Inject
    DefaultDependencyResolver( RepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public DependencyResolverResult resolve( DependencyResolverRequest request )
            throws DependencyResolverException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();

        CollectRequest collectRequest = new CollectRequest()
                .setRootArtifact( request.getRootArtifact().map( session::toArtifact ).orElse( null ) )
                .setRoot( request.getRoot().map( session::toDependency ).orElse( null ) )
                .setDependencies( session.toDependencies( request.getDependencies() ) )
                .setManagedDependencies( session.toDependencies( request.getManagedDependencies() ) )
                .setRepositories( session.toRepositories( request.getSession().getRemoteRepositories() ) );
        DependencyFilter filter = request.getFilter()
                .map( f -> toDependencyFilter( session, f ) )
                .orElse( null );
        DependencyRequest dependencyRequest = new DependencyRequest()
                .setCollectRequest( collectRequest )
                .setFilter( filter );

        try
        {
            DependencyResult result = repositorySystem.resolveDependencies( session.getSession(), dependencyRequest );
            return new DependencyResolverResult()
            {
                @Override
                public List<Exception> getCollectorExceptions()
                {
                    return result.getCollectExceptions();
                }

                @Override
                public Node getRoot()
                {
                    return session.getNode( result.getRoot() );
                }

                @Override
                public List<ArtifactResolverResult> getArtifactResults()
                {
                    return null;
                }
            };
        }
        catch ( DependencyResolutionException e )
        {
            throw new DependencyResolverException( "Unable to resolve dependencies", e );
        }
    }

    private DependencyFilter toDependencyFilter( DefaultSession session, Predicate<Node> filter )
    {
        return ( node, parents ) -> filter.test( session.getNode( node ) );
    }
}
