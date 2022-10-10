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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;

/**
 *
 * @since 4.0
 */
@Experimental
@Immutable
public interface DependencyCoordinateFactoryRequest extends ArtifactCoordinateFactoryRequest
{

    String getScope();

    boolean isOptional();

    @Nonnull
    Collection<Exclusion> getExclusions();

    static DependencyCoordinateFactoryRequest build( Session session, String groupId, String artifactId,
                                                   String version, String classifier, String extension, String type )
    {
        return DependencyCoordinateFactoryRequest.builder()
                .session( session )
                .groupId( groupId )
                .artifactId( artifactId )
                .version( version )
                .classifier( classifier )
                .extension( extension )
                .type( type )
                .build();
    }

    static DependencyCoordinateFactoryRequest build( Session session, ArtifactCoordinate coordinate )
    {
        return builder()
                .session( session )
                .groupId( coordinate.getGroupId() )
                .artifactId( coordinate.getArtifactId() )
                .version( coordinate.getVersion().asString() )
                .classifier( coordinate.getClassifier() )
                .extension( coordinate.getExtension() )
                .build();
    }

    static DependencyCoordinateFactoryRequest build( Session session, Dependency dependency )
    {
        return builder()
                .session( session )
                .groupId( dependency.getGroupId() )
                .artifactId( dependency.getArtifactId() )
                .version( dependency.getVersion().asString() )
                .classifier( dependency.getClassifier() )
                .extension( dependency.getExtension() )
                .type( dependency.getType().getName() )
                .scope( dependency.getScope().id() )
                .optional( dependency.isOptional() )
                .build();
    }

    static DependencyCoordinateFactoryRequestBuilder builder()
    {
        return new DependencyCoordinateFactoryRequestBuilder();
    }

    @NotThreadSafe
    class DependencyCoordinateFactoryRequestBuilder
    {
        private Session session;
        private String groupId;
        private String artifactId;
        private String version;
        private String classifier;
        private String extension;
        private String type;
        private String scope;
        private boolean optional;
        private Collection<Exclusion> exclusions = Collections.emptyList();

        DependencyCoordinateFactoryRequestBuilder()
        {
        }

        public DependencyCoordinateFactoryRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder groupId( String groupId )
        {
            this.groupId = groupId;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder artifactId( String artifactId )
        {
            this.artifactId = artifactId;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder version( String version )
        {
            this.version = version;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder classifier( String classifier )
        {
            this.classifier = classifier;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder extension( String extension )
        {
            this.extension = extension;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder type( String type )
        {
            this.type = type;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder scope( String scope )
        {
            this.scope = scope;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder optional( boolean optional )
        {
            this.optional = optional;
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder exclusions( Collection<Exclusion> exclusions )
        {
            if ( exclusions != null )
            {
                if ( this.exclusions.isEmpty() )
                {
                    this.exclusions = new ArrayList<>();
                }
                this.exclusions.addAll( exclusions );
            }
            return this;
        }

        public DependencyCoordinateFactoryRequestBuilder exclusion( Exclusion exclusion )
        {
            if ( exclusion != null )
            {
                if ( this.exclusions.isEmpty() )
                {
                    this.exclusions = new ArrayList<>();
                }
                this.exclusions.add( exclusion );
            }
            return this;
        }

        public DependencyCoordinateFactoryRequest build()
        {
            return new DefaultDependencyCoordinateFactoryRequest( session, groupId, artifactId, version,
                    classifier, extension, type, scope, optional, exclusions );
        }

        private static class DefaultDependencyCoordinateFactoryRequest
                extends BaseRequest
                implements DependencyCoordinateFactoryRequest
        {
            private final String groupId;
            private final String artifactId;
            private final String version;
            private final String classifier;
            private final String extension;
            private final String type;
            private final String scope;
            private final boolean optional;
            private final Collection<Exclusion> exclusions;

            @SuppressWarnings( "checkstyle:ParameterNumber" )
            private DefaultDependencyCoordinateFactoryRequest(
                            @Nonnull Session session, String groupId,
                            String artifactId,
                            String version,
                            String classifier,
                            String extension,
                            String type,
                            String scope,
                            boolean optional,
                            Collection<Exclusion> exclusions )
            {
                super( session );
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                this.classifier = classifier;
                this.extension = extension;
                this.type = type;
                this.scope = scope;
                this.optional = optional;
                this.exclusions = exclusions;
            }

            @Override
            public String getGroupId()
            {
                return groupId;
            }

            @Override
            public String getArtifactId()
            {
                return artifactId;
            }

            @Override
            public String getVersion()
            {
                return version;
            }

            @Override
            public String getClassifier()
            {
                return classifier;
            }

            @Override
            public String getExtension()
            {
                return extension;
            }

            @Override
            public String getType()
            {
                return type;
            }

            @Override
            public String getScope()
            {
                return scope;
            }

            @Override
            public boolean isOptional()
            {
                return optional;
            }

            @Nonnull
            @Override
            public Collection<Exclusion> getExclusions()
            {
                return exclusions;
            }
        }
    }

}
