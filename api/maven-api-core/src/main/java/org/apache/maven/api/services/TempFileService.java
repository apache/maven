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

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service to create and track temporary files/directories for a Maven build.
 * All created paths are deleted automatically when the session ends.
 */
public interface TempFileService extends Service {

    /**
     * Creates a temp file in the default temp directory.
     */
    @Nonnull
    Path createTempFile(Session session, String prefix, String suffix) throws IOException;

    /**
     * Creates a temp file in the given directory.
     */
    @Nonnull
    Path createTempFile(Session session, String prefix, String suffix, Path directory) throws IOException;

    /**
     * Creates a temp directory in the default temp directory.
     */
    @Nonnull
    Path createTempDirectory(Session session, String prefix) throws IOException;

    /**
     * Creates a temp directory in the given directory.
     */
    @Nonnull
    Path createTempDirectory(Session session, String prefix, Path directory) throws IOException;

    /**
     * Registers an externally created path for cleanup at session end.
     */
    @Nonnull
    void register(Session session, Path path);

    /**
     * Forces cleanup for the given session (normally called by lifecycle).
     */
    @Nonnull
    void cleanup(Session session) throws IOException;
}
