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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static org.apache.maven.api.services.BaseRequest.nonNull;

public interface ToolchainsBuilderRequest
{
    @Nonnull
    Session getSession();

    /**
     * Gets the global Toolchains path.
     *
     * @return The global Toolchains path or {@code null} if none.
     */
    @Nonnull
    Optional<Path> getGlobalToolchainsPath();

    /**
     * Gets the global Toolchains source.
     *
     * @return The global Toolchains source or {@code null} if none.
     */
    @Nonnull
    Optional<Source> getGlobalToolchainsSource();

    /**
     * Gets the user Toolchains path.
     *
     * @return The user Toolchains path or {@code null} if none.
     */
    @Nonnull
    Optional<Path> getUserToolchainsPath();

    /**
     * Gets the user Toolchains source.
     *
     * @return The user Toolchains source or {@code null} if none.
     */
    @Nonnull
    Optional<Source> getUserToolchainsSource();

    @Nonnull
    static ToolchainsBuilderRequest build( @Nonnull Session session,
                                         @Nonnull Source globalToolchainsSource,
                                         @Nonnull Source userToolchainsSource )
    {
        return builder()
                .session( nonNull( session, "session can not be null" ) )
                .globalToolchainsSource( nonNull( globalToolchainsSource, "globalToolchainsSource can not be null" ) )
                .userToolchainsSource( nonNull( userToolchainsSource, "userToolchainsSource can not be null" ) )
                .build();
    }

    @Nonnull
    static ToolchainsBuilderRequest build( @Nonnull Session session,
                                         @Nonnull Path globalToolchainsPath,
                                         @Nonnull Path userToolchainsPath )
    {
        return builder()
                .session( nonNull( session, "session can not be null" ) )
                .globalToolchainsPath( nonNull( globalToolchainsPath, "globalToolchainsPath can not be null" ) )
                .userToolchainsPath( nonNull( userToolchainsPath, "userToolchainsPath can not be null" ) )
                .build();
    }

    @Nonnull
    static ToolchainsBuilderRequestBuilder builder()
    {
        return new ToolchainsBuilderRequestBuilder();
    }

    @NotThreadSafe
    class ToolchainsBuilderRequestBuilder
    {
        Session session;
        Path globalToolchainsPath;
        Source globalToolchainsSource;
        Path userToolchainsPath;
        Source userToolchainsSource;

        public ToolchainsBuilderRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        public ToolchainsBuilderRequestBuilder globalToolchainsPath( Path globalToolchainsPath )
        {
            this.globalToolchainsPath = globalToolchainsPath;
            return this;
        }

        public ToolchainsBuilderRequestBuilder globalToolchainsSource( Source globalToolchainsSource )
        {
            this.globalToolchainsSource = globalToolchainsSource;
            return this;
        }

        public ToolchainsBuilderRequestBuilder userToolchainsPath( Path userToolchainsPath )
        {
            this.userToolchainsPath = userToolchainsPath;
            return this;
        }

        public ToolchainsBuilderRequestBuilder userToolchainsSource( Source userToolchainsSource )
        {
            this.userToolchainsSource = userToolchainsSource;
            return this;
        }

        public ToolchainsBuilderRequest build()
        {
            return new ToolchainsBuilderRequestBuilder.DefaultToolchainsBuilderRequest( session,
                    globalToolchainsPath, globalToolchainsSource,
                    userToolchainsPath, userToolchainsSource );
        }

        private static class DefaultToolchainsBuilderRequest extends BaseRequest
                implements ToolchainsBuilderRequest
        {
            private final Path globalToolchainsPath;
            private final Source globalToolchainsSource;
            private final Path userToolchainsPath;
            private final Source userToolchainsSource;

            @SuppressWarnings( "checkstyle:ParameterNumber" )
            DefaultToolchainsBuilderRequest( @Nonnull Session session,
                                             @Nullable Path globalToolchainsPath,
                                             @Nullable Source globalToolchainsSource,
                                             @Nullable Path userToolchainsPath,
                                             @Nullable Source userToolchainsSource )
            {
                super( session );
                this.globalToolchainsPath = globalToolchainsPath;
                this.globalToolchainsSource = globalToolchainsSource;
                this.userToolchainsPath = userToolchainsPath;
                this.userToolchainsSource = userToolchainsSource;
            }

            @Nonnull
            @Override
            public Optional<Path> getGlobalToolchainsPath()
            {
                return Optional.ofNullable( globalToolchainsPath );
            }

            @Nonnull
            @Override
            public Optional<Source> getGlobalToolchainsSource()
            {
                return Optional.ofNullable( globalToolchainsSource );
            }

            @Nonnull
            @Override
            public Optional<Path> getUserToolchainsPath()
            {
                return Optional.ofNullable( userToolchainsPath );
            }

            @Nonnull
            @Override
            public Optional<Source> getUserToolchainsSource()
            {
                return Optional.ofNullable( userToolchainsSource );
            }
        }
    }
}
