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

import java.nio.file.Path;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * <p>The <dfn>local repository</dfn> is a directory on the developer's machine where
 * Maven stores all the downloaded artifacts (such as dependencies, plugins,
 * and project artifacts). When Maven builds a project, it first checks the
 * local repository to see if the required artifacts are already available.
 * If the artifacts are found locally, Maven uses them directly, which speeds
 * up the build process by avoiding unnecessary downloads.</p>
 *
 * <p>By default, the local repository is located in the {@code .m2/repository}
 * directory within the user's home directory ({@code ~/.m2/repository} on
 * Unix-like systems or {@code C:\Users\YourName\.m2\repository} on Windows).
 * The location of the local repository can be customized in the
 * {@code settings.xml} file.</p>
 *
 * @since 4.0.0
 * @see Repository
 * @see org.apache.maven.api.settings.Settings
 */
@Experimental
@Immutable
public interface LocalRepository extends Repository {

    @Nonnull
    Path getPath();
}
