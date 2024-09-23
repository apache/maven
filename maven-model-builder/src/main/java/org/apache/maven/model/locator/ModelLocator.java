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
package org.apache.maven.model.locator;

import java.io.File;
import java.nio.file.Path;

/**
 * Locates a POM file within a project base directory.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
public interface ModelLocator {

    /**
     * Locates the POM file within the specified project directory. In case the given project directory does not exist
     * or does not contain a POM file, the return value indicates the expected path to the POM file. Subdirectories of
     * the project directory will not be considered when locating the POM file. The return value will be an absolute
     * path if the project directory is given as an absolute path.
     *
     * @param projectDirectory The (possibly non-existent) base directory to locate the POM file in, must not be {@code
     *            null}.
     * @return The path to the (possibly non-existent) POM file, never {@code null}.
     * @deprecated Use {@link #locatePom(Path)} instead.
     */
    @Deprecated
    File locatePom(File projectDirectory);

    /**
     * Locates the POM file within the specified project directory. In case the given project directory does not exist
     * or does not contain a POM file, the return value indicates the expected path to the POM file. Subdirectories of
     * the project directory will not be considered when locating the POM file. The return value will be an absolute
     * path if the project directory is given as an absolute path.
     *
     * @param projectDirectory The (possibly non-existent) base directory to locate the POM file in, must not be {@code
     *            null}.
     * @return The path to the (possibly non-existent) POM file, never {@code null}.
     * @since 4.0.0
     */
    Path locatePom(Path projectDirectory);

    /**
     * Returns the file containing the pom or null if a pom can not be found at the given file or in the given directory.
     *
     * @deprecated Use {@link #locateExistingPom(Path)} instead.
     */
    @Deprecated
    default File locateExistingPom(File project) {
        Path path = locateExistingPom(project != null ? project.toPath() : null);
        return path != null ? path.toFile() : null;
    }

    /**
     * Returns the file containing the pom or null if a pom can not be found at the given file or in the given directory.
     *
     * @since 4.0.0
     */
    Path locateExistingPom(Path project);
}
