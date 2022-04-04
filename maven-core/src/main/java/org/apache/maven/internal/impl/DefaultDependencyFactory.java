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

import javax.inject.Named;

import java.util.stream.Collectors;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.DependencyFactory;
import org.apache.maven.api.services.DependencyFactoryRequest;

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
public class DefaultDependencyFactory implements DependencyFactory
{

    @Nonnull
    @Override
    public Dependency create( @Nonnull DependencyFactoryRequest request )
    {
        nonNull( request, "request can not be null" );
        DefaultSession session = cast( DefaultSession.class, request.getSession(),
                "request.session should be a " + DefaultSession.class );

        return new DefaultDependency(
                session,
                new org.eclipse.aether.graph.Dependency(
                        session.toArtifact( request.getArtifact() ),
                        request.getScope(),
                        request.isOptional(),
                        request.getExclusions().stream().map( this::toExclusion ).collect( Collectors.toList() ) ) );
    }

    private org.eclipse.aether.graph.Exclusion toExclusion( Exclusion exclusion )
    {
        return new org.eclipse.aether.graph.Exclusion(
                exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*" );
    }

}
