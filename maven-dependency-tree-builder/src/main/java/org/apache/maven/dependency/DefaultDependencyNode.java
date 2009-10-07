package org.apache.maven.dependency;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

/**
 * @author Benjamin Bentmann
 */
class DefaultDependencyNode
    implements DependencyNode
{

    private Dependency dependency;

    private Model metadata;

    private String repositoryId;

    private List<Model> relocations;

    private DependencyNode parent;

    private List<DependencyNode> children;

    private int depth;

    public DefaultDependencyNode( Dependency dependency, Model metadata, String repositoryId, List<Model> relocations,
                                  DependencyNode parent )
    {
        this.dependency = dependency;
        this.metadata = metadata;
        this.repositoryId = repositoryId;
        this.relocations = relocations;
        this.parent = parent;
        this.children = new ArrayList<DependencyNode>();
        this.depth = ( parent != null ) ? parent.getDepth() + 1 : 0;
    }

    public Dependency getDependency()
    {
        return dependency;
    }

    public Model getMetadata()
    {
        return metadata;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public List<Model> getRelocations()
    {
        return relocations;
    }

    public DependencyNode getParent()
    {
        return parent;
    }

    public List<DependencyNode> getChildren()
    {
        return children;
    }

    public DefaultDependencyNode addChild( Dependency dependency, Model metadata, String repositoryId,
                                           List<Model> relocations )
    {
        DefaultDependencyNode child = new DefaultDependencyNode( dependency, metadata, repositoryId, relocations, this );

        children.add( child );

        return child;
    }

    public int getDepth()
    {
        return depth;
    }

}
