package org.apache.maven.api.services;

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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Coordinate;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;

/**
 * Service used to create {@link Coordinate} objects.
 *
 * @since 4.0
 */
@Experimental
public interface CoordinateFactory extends Service
{

    /**
     * Creates a coordinate.
     *
     * @param request the request holding coordinate creation parameters
     * @return an {@code Artifact}, never {@code null}
     * @throws IllegalArgumentException if {@code request} is null or {@code request.session} is null or invalid
     */
    @Nonnull
    Coordinate create( @Nonnull CoordinateFactoryRequest request );

    @Nonnull
    default Coordinate create( @Nonnull Session session, String groupId,
                               String artifactId, String version, String extension )
    {
        return create( CoordinateFactoryRequest.build( session, groupId, artifactId, version, extension ) );
    }

    @Nonnull
    default Coordinate create( @Nonnull Session session, String groupId, String artifactId, String version,
                               String classifier, String extension, String type )
    {
        return create( CoordinateFactoryRequest.build( session, groupId, artifactId,
                                                     version, classifier, extension, type ) );
    }

    @Nonnull
    default Coordinate create( @Nonnull Session session, Dependency dependency )
    {
        return create( CoordinateFactoryRequest.build( session,
                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null ) );
    }

    @Nonnull
    default Coordinate create( @Nonnull Session session, Artifact artifact )
    {
        return create( CoordinateFactoryRequest.build( session,
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion().asString(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getType().getName() ) );
    }

    @Nonnull
    default Coordinate create( @Nonnull Session session, Plugin plugin )
    {
        return create( CoordinateFactoryRequest.build( session,
                plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null ) );
    }

    @Nonnull
    default Coordinate create( @Nonnull Session session, ReportPlugin reportPlugin )
    {
        return create( CoordinateFactoryRequest.build( session,
                reportPlugin.getGroupId(), reportPlugin.getArtifactId(), reportPlugin.getVersion(), null ) );
    }
}
