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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * <p>In Maven, repositories are locations where project artifacts (such as JAR files, POM files, and other
 * resources) are stored and retrieved. There are two primary types of repositories:
 * {@linkplain LocalRepository local repositories} and
 * {@linkplain RemoteRepository remote repositories}.</p>
 *
 * <h2>Repository Resolution Process</h2>
 *
 * <p>When resolving dependencies, Maven follows this order:</p><ol>
 * <li>Check Local Repository: Maven first checks if the artifact is available in the local repository.</li>
 * <li>Check Remote Repositories: If the artifact is not found locally, Maven queries the configured remote repositories in the order they are listed.</li>
 * <li>Download and Cache: If Maven finds the artifact in a remote repository, it downloads it and stores it in the local repository for future use.</li>
 * </ol>
 * <p>By caching artifacts in the local repository, Maven minimizes the need to repeatedly download the same artifacts, thus optimizing the build process.</p>
 *
 * <h2>Repository Configuration</h2>
 *
 * <p>Repositories can be configured at various levels:<ol>
 * <li>POM: Repositories can be specified in the {@code pom.xml} file under the {@code <repositories>} and {@code <pluginRepositories>} sections.</li>
 * <li>Settings: the {@code settings.xml} can be used to provide additional repositories in the three level of settings (user, project, installation).</li>
 * </ol>
 * By understanding and properly configuring repositories, developers can control where Maven looks for dependencies, manage access to proprietary artifacts, and optimize the build process to ensure consistency and reliability across projects.
 *
 * @since 4.0.0
 * @see RemoteRepository
 * @see LocalRepository
 */
@Experimental
@Immutable
public interface Repository {

    /**
     * The reserved id for Maven Central
     */
    String CENTRAL_ID = "central";

    /**
     * Gets the identifier of this repository.
     *
     * @return the (case-sensitive) identifier, never {@code null}
     */
    @Nonnull
    String getId();

    /**
     * Gets the type of the repository, for example "default".
     *
     * @return the (case-sensitive) type of the repository, never {@code null}
     */
    @Nonnull
    String getType();
}
