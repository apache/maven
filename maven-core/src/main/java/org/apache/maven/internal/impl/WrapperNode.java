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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.eclipse.aether.graph.DependencyNode;

class WrapperNode extends AbstractNode
{
    protected final Node delegate;
    protected final List<Node> children;

    WrapperNode( Node delegate, List<Node> children )
    {
        this.delegate = delegate;
        this.children = children;
    }

    @Override
    DependencyNode getDependencyNode()
    {
        return Utils.cast( AbstractNode.class, delegate,
                "delegate is not an instance of AbstractNode" ).getDependencyNode();
    }

    @Override
    public List<Node> getChildren()
    {
        return children;
    }

    @Override
    public Artifact getArtifact()
    {
        return delegate.getArtifact();
    }

    @Override
    public Dependency getDependency()
    {
        return delegate.getDependency();
    }

    @Override
    @Nonnull
    public List<RemoteRepository> getRemoteRepositories()
    {
        return delegate.getRemoteRepositories();
    }

    @Override
    @Nonnull
    public Optional<RemoteRepository> getRepository()
    {
        return delegate.getRepository();
    }

    @Override
    @Nonnull
    public String asString()
    {
        return delegate.asString();
    }
}
