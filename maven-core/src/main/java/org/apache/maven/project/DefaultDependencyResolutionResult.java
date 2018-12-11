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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

/**
 * @author Benjamin Bentmann
 */
class DefaultDependencyResolutionResult
    implements DependencyResolutionResult
{

    private DependencyNode root;

    private List<Dependency> dependencies = new ArrayList<>();

    private List<Dependency> resolvedDependencies = new ArrayList<>();

    private List<Dependency> unresolvedDependencies = new ArrayList<>();

    private List<Exception> collectionErrors = new ArrayList<>();

    private Map<Dependency, List<Exception>> resolutionErrors = new IdentityHashMap<>();

    public DependencyNode getDependencyGraph()
    {
        return root;
    }

    public void setDependencyGraph( DependencyNode root )
    {
        this.root = root;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public List<Dependency> getResolvedDependencies()
    {
        return resolvedDependencies;
    }

    public void addResolvedDependency( Dependency dependency )
    {
        dependencies.add( dependency );
        resolvedDependencies.add( dependency );
    }

    public List<Dependency> getUnresolvedDependencies()
    {
        return unresolvedDependencies;
    }

    public List<Exception> getCollectionErrors()
    {
        return collectionErrors;
    }

    public void setCollectionErrors( List<Exception> exceptions )
    {
        if ( exceptions != null )
        {
            this.collectionErrors = exceptions;
        }
        else
        {
            this.collectionErrors = new ArrayList<>();
        }
    }

    public List<Exception> getResolutionErrors( Dependency dependency )
    {
        List<Exception> errors = resolutionErrors.get( dependency );
        return ( errors != null )
                   ? Collections.unmodifiableList( errors )
                   : Collections.<Exception>emptyList();

    }

    public void setResolutionErrors( Dependency dependency, List<Exception> errors )
    {
        dependencies.add( dependency );
        unresolvedDependencies.add( dependency );
        resolutionErrors.put( dependency, errors );
    }

}
