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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

public class DefaultNode extends AbstractNode
{

    protected final @Nonnull AbstractSession session;
    protected final @Nonnull org.eclipse.aether.graph.DependencyNode node;
    protected final boolean verbose;

    public DefaultNode( @Nonnull AbstractSession session,
                        @Nonnull org.eclipse.aether.graph.DependencyNode node,
                        boolean verbose )
    {
        this.session = session;
        this.node = node;
        this.verbose = verbose;
    }

    @Override
    DependencyNode getDependencyNode()
    {
        return node;
    }

    @Override
    public Dependency getDependency()
    {
        return node.getDependency() != null ? session.getDependency( node.getDependency() ) : null;
    }

    @Override
    public List<Node> getChildren()
    {
        return new MappedList<>( node.getChildren(), n -> session.getNode( n, verbose ) );
    }

    @Override
    public List<RemoteRepository> getRemoteRepositories()
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
    public String asString()
    {
        String nodeString = super.asString();

        if ( !verbose )
        {
            return nodeString;
        }

        org.eclipse.aether.graph.DependencyNode node = getDependencyNode();

        List<String> details = new ArrayList<>();

        org.eclipse.aether.graph.DependencyNode winner =
                (org.eclipse.aether.graph.DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
        String winnerVersion = winner != null ? winner.getArtifact().getBaseVersion() : null;
        boolean included = ( winnerVersion == null );

        String preManagedVersion = DependencyManagerUtils.getPremanagedVersion( node );
        if ( preManagedVersion != null )
        {
            details.add( "version managed from " + preManagedVersion );
        }

        String preManagedScope = DependencyManagerUtils.getPremanagedScope( node );
        if ( preManagedScope != null )
        {
            details.add( "scope managed from " + preManagedScope );
        }

        String originalScope = (String) node.getData().get( ConflictResolver.NODE_DATA_ORIGINAL_SCOPE );
        if ( originalScope != null && !originalScope.equals( node.getDependency().getScope() ) )
        {
            details.add( "scope updated from " + originalScope );
        }

        if ( !included )
        {
            if ( Objects.equals( winnerVersion, node.getArtifact().getVersion() ) )
            {
                details.add( "omitted for duplicate" );
            }
            else
            {
                details.add( "omitted for conflict with " + winnerVersion );
            }
        }

        StringBuilder buffer = new StringBuilder();
        if ( included )
        {
            buffer.append( nodeString );
            if ( !details.isEmpty() )
            {
                buffer.append( " (" );
                join( buffer, details, "; " );
                buffer.append( ")" );
            }
        }
        else
        {
            buffer.append( "(" );
            buffer.append( nodeString );
            if ( !details.isEmpty() )
            {
                buffer.append( " - " );
                join( buffer, details, "; " );
            }
            buffer.append( ")" );
        }
        return buffer.toString();
    }

    private static void join( StringBuilder buffer, List<String> details, String separator )
    {
        boolean first = true;
        for ( String detail : details )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                buffer.append( separator );
            }
            buffer.append( detail );
        }
    }

}
