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

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Exclusion;

public interface DependencyFactoryRequest
{

    @Nonnull
    Session getSession();

    Artifact getArtifact();

    String getScope();

    boolean isOptional();

    @Nonnull
    Collection<Exclusion> getExclusions();

    static DependencyFactoryRequest build( Session session, Artifact artifact )
    {
        return builder()
                .session( session )
                .artifact( artifact )
                .build();
    }

    static DependencyFactoryRequestBuilder builder()
    {
        return new DependencyFactoryRequestBuilder();
    }

    class DependencyFactoryRequestBuilder
    {
        private Session session;
        private Artifact artifact;
        private String scope;
        private boolean optional;
        private Collection<Exclusion> exclusions = Collections.emptyList();

        public DependencyFactoryRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        public DependencyFactoryRequestBuilder artifact( Artifact artifact )
        {
            this.artifact = artifact;
            return this;
        }

        public DependencyFactoryRequestBuilder scope( String scope )
        {
            this.scope = scope;
            return this;
        }

        public DependencyFactoryRequestBuilder optional( boolean optional )
        {
            this.optional = optional;
            return this;
        }

        public DependencyFactoryRequestBuilder exclusions( Collection<Exclusion> exclusions )
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

        public DependencyFactoryRequestBuilder exclusion( Exclusion exclusion )
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

        public DependencyFactoryRequest build()
        {
            return new DefaultDependencyFactoryRequest( session, artifact, scope, optional, exclusions );
        }

        private static class DefaultDependencyFactoryRequest extends BaseRequest implements DependencyFactoryRequest
        {
            private final Artifact artifact;
            private final String scope;
            private final boolean optional;
            private final Collection<Exclusion> exclusions;

            private DefaultDependencyFactoryRequest( @Nonnull Session session, Artifact artifact, String scope,
                                                     boolean optional, Collection<Exclusion> exclusions )
            {
                super( session );
                this.artifact = artifact;
                this.scope = scope;
                this.optional = optional;
                this.exclusions = exclusions;
            }

            @Override
            public Artifact getArtifact()
            {
                return artifact;
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
