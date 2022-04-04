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
import org.apache.maven.api.annotations.Nullable;

import java.util.Collection;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultDependency implements Dependency
{
    private final DefaultSession session;
    private final org.eclipse.aether.graph.Dependency dependency;

    public DefaultDependency( @Nonnull DefaultSession session, @Nonnull org.eclipse.aether.graph.Dependency dependency )
    {
        this.session = nonNull( session, "session" );
        this.dependency = nonNull( dependency, "dependency" );
    }

    @Nonnull
    public org.eclipse.aether.graph.Dependency getDependency()
    {
        return dependency;
    }

    @Nonnull
    @Override
    public Artifact getArtifact()
    {
        return session.getArtifact( dependency.getArtifact() );
    }

    @Nonnull
    @Override
    public String getScope()
    {
        return dependency.getScope();
    }

    @Nullable
    @Override
    public Boolean getOptional()
    {
        return dependency.getOptional();
    }

    @Nonnull
    @Override
    public Collection<Exclusion> getExclusions()
    {
        return new MappedCollection<>( dependency.getExclusions(), this::toExclusion );
    }

    private Exclusion toExclusion( org.eclipse.aether.graph.Exclusion exclusion )
    {
        return new Exclusion()
        {
            @Nullable
            @Override
            public String getGroupId()
            {
                return exclusion.getGroupId();
            }

            @Nullable
            @Override
            public String getArtifactId()
            {
                return exclusion.getArtifactId();
            }
        };
    }
}
