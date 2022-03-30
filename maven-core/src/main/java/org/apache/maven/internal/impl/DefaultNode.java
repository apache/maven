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

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.NodeVisitor;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Repository;

public class DefaultNode implements Node
{

    private final DefaultSession session;
    private final org.eclipse.aether.graph.DependencyNode node;

    public DefaultNode( DefaultSession session,
                        org.eclipse.aether.graph.DependencyNode node )
    {
        this.session = session;
        this.node = node;
    }

    @Override
    public Artifact getArtifact()
    {
        return session.getArtifact( node.getArtifact() );
    }

    @Override
    public Dependency getDependency()
    {
        return session.getDependency( node.getDependency() );
    }

    @Override
    public List<Node> getChildren()
    {
        return new MappedList<>( node.getChildren(), session::getNode );
    }

    @Override
    public List<Repository> getRemoteRepositories()
    {
        return new MappedList<>( node.getRepositories(), session::getRemoteRepository );
    }

    @Override
    public Optional<RemoteRepository> getRepository()
    {
        // TODO
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    @Override
    public boolean accept( NodeVisitor visitor )
    {
        if ( visitor.enter( this ) )
        {
            for ( Node child : getChildren() )
            {
                if ( !child.accept( visitor ) )
                {
                    break;
                }
            }
        }
        return visitor.leave( this );
    }

    @Override
    public Node filter( Predicate<Node> filter )
    {
        // TODO
        throw new UnsupportedOperationException( "Not implemented yet" );
    }
}
