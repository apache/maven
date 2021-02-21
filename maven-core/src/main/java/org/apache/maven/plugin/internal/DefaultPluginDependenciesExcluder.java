package org.apache.maven.plugin.internal;

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

import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExportsProvider;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of {@link PluginDependenciesExcluder}.
 *
 * @since TBD
 */
@Named
@Singleton
public class DefaultPluginDependenciesExcluder implements PluginDependenciesExcluder
{
    private final CoreExports coreExports;

    @Inject
    public DefaultPluginDependenciesExcluder( final CoreExportsProvider coreExportsProvider )
    {
        Objects.requireNonNull( coreExportsProvider );
        this.coreExports = coreExportsProvider.get();
    }

    @Override
    public DependencyFilter coreDependencyFilter()
    {
        return new CoreDependencyFilter( coreExports.getExportedArtifacts() );
    }

    @Override
    public DependencySelector coreDependencySelector()
    {
        return new CoreDependencySelector( coreExports.getExportedArtifacts() );
    }

    private static class CoreDependencyFilter
            implements DependencyFilter
    {
        private final Set<String> coreArtifacts;

        private CoreDependencyFilter( final Set<String> coreArtifacts )
        {
            this.coreArtifacts = coreArtifacts;
        }

        @Override
        public boolean accept( final DependencyNode dependencyNode, final List<DependencyNode> list )
        {
            final Artifact artifact = dependencyNode.getArtifact();
            return !coreArtifacts.contains( artifact.getGroupId() + ":" + artifact.getArtifactId() );
        }
    }

    private static class CoreDependencySelector
            implements DependencySelector
    {
        private final Set<String> coreArtifacts;

        private CoreDependencySelector( final Set<String> coreArtifacts )
        {
            this.coreArtifacts = coreArtifacts;
        }

        @Override
        public boolean selectDependency( final Dependency dependency )
        {
            final Artifact artifact = dependency.getArtifact();
            return !coreArtifacts.contains( artifact.getGroupId() + ":" + artifact.getArtifactId() );
        }

        @Override
        public DependencySelector deriveChildSelector( final DependencyCollectionContext dependencyCollectionContext )
        {
            return this;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == this )
            {
                return true;
            }
            else if ( obj == null || !getClass().equals( obj.getClass() ) )
            {
                return false;
            }

            CoreDependencySelector that = (CoreDependencySelector) obj;
            return coreArtifacts.equals( that.coreArtifacts );
        }

        @Override
        public int hashCode()
        {
            return getClass().hashCode() * 31 + coreArtifacts.hashCode();
        }
    }
}
