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
package org.apache.maven.model.path;

import java.io.File;
import java.nio.file.Path;

/**
 * Resolves relative paths against a specific base directory.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
public interface PathTranslator {

    /**
     * Resolves the specified path against the given base directory. The resolved path will be absolute and uses the
     * platform-specific file separator if a base directory is given. Otherwise, the input path will be returned
     * unaltered.
     *
     * @param path The path to resolve, may be {@code null}.
     * @param basedir The base directory to resolve relative paths against, may be {@code null}.
     * @return The resolved path or {@code null} if the input path was {@code null}.
     * @deprecated Use {@link #alignToBaseDirectory(String, Path)} instead.
     */
    @Deprecated
    String alignToBaseDirectory(String path, File basedir);

    /**
     * Resolves the specified path against the given base directory. The resolved path will be absolute and uses the
     * platform-specific file separator if a base directory is given. Otherwise, the input path will be returned
     * unaltered.
     *
     * @param path The path to resolve, may be {@code null}.
     * @param basedir The base directory to resolve relative paths against, may be {@code null}.
     * @return The resolved path or {@code null} if the input path was {@code null}.
     * @since 4.0.0
     */
    String alignToBaseDirectory(String path, Path basedir);
}
