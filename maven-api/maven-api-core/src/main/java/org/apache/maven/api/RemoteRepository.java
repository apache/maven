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
import org.apache.maven.api.model.ModelBase;

/**
 * <p>A <dfn>remote repository</dfn> is a central or distributed location
 * from which Maven can download project dependencies, plugins, and other
 * build artifacts. When Maven cannot find an artifact in the local
 * repository, it attempts to retrieve it from one or more remote
 * repositories.</p>
 *
 * <p>There are several types of remote repositories:</p><ul>
 * <li><dfn>Central Repository</dfn>: The default remote repository used by Maven. It is a large, publicly accessible repository maintained by the Maven community at https://repo.maven.apache.org/maven2. Most common Java libraries and frameworks are hosted here.</li>
 * <li><dfn>Private Remote Repository</dfn>: Organizations often maintain their own private remote repositories, which may host proprietary or custom-built artifacts that are not available in the central repository. These repositories can be managed using tools like Apache Archiva, Sonatype Nexus, or JFrog Artifactory.</li>
 * <li><dfn>Third-Party Repositories</dfn>: Some projects or organizations host their own remote repositories for distributing specific artifacts that are not available in the central repository. These repositories must be explicitly added to the Maven pom.xml or settings.xml files for Maven to access them.</li></ul>
 *
 * <h2>Repository Configuration</h2>
 *
 * <p>Repositories can be configured at various levels:</p><ol>
 * <li>POM: Repositories can be specified in the {@code pom.xml} file under the {@code <repositories>} and {@code <pluginRepositories>} sections.</li>
 * <li>Settings: the {@code settings.xml} can be used to provide additional repositories in the three level of settings (user, project, installation).</li>
 * </ol>
 * <p>By understanding and properly configuring repositories, developers can control where Maven looks for dependencies, manage access to proprietary artifacts, and optimize the build process to ensure consistency and reliability across projects.
 * </p>
 *
 *
 * @since 4.0.0
 * @see Repository
 * @see LocalRepository
 * @see Session#getSettings()
 * @see ModelBase#getRepositories()
 * @see ModelBase#getPluginRepositories()
 */
@Experimental
@Immutable
public interface RemoteRepository extends Repository {

    @Nonnull
    String getUrl();

    @Nonnull
    String getProtocol();
}
