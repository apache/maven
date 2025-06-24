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
package org.apache.maven.api.services;

import java.nio.file.Path;

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * @since 4.0.0
 */
@Experimental
public interface ProjectBuilder extends Service {

    /**
     * Creates a {@link org.apache.maven.api.Project} from a POM file.
     *
     * @param request {@link ProjectBuilderRequest}
     * @return the {@link ProjectBuilderResult} containing the built project and possible errors
     * @throws ProjectBuilderException if the project cannot be created
     * @throws IllegalArgumentException if an argument is {@code null} or invalid
     */
    @Nonnull
    ProjectBuilderResult build(ProjectBuilderRequest request) throws ProjectBuilderException;

    /**
     * Creates a {@link org.apache.maven.api.Project} from a POM file.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param source The {@link Source}, must not be {@code null}
     * @throws ProjectBuilderException if the project cannot be created
     * @throws IllegalArgumentException if an argument is {@code null} or invalid
     * @see #build(ProjectBuilderRequest)
     */
    @Nonnull
    default ProjectBuilderResult build(@Nonnull Session session, @Nonnull Source source)
            throws ProjectBuilderException {
        return build(ProjectBuilderRequest.build(session, source));
    }

    /**
     * Creates a {@link org.apache.maven.api.Project} from a POM file.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param path the {@link Path}, must not be {@code null}
     * @throws ProjectBuilderException if the project cannot be created
     * @throws IllegalArgumentException if an argument is {@code null} or invalid
     * @see #build(ProjectBuilderRequest)
     */
    @Nonnull
    default ProjectBuilderResult build(@Nonnull Session session, @Nonnull Path path) throws ProjectBuilderException {
        return build(ProjectBuilderRequest.build(session, path));
    }
}
