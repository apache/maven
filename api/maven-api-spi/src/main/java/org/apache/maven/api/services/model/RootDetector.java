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
package org.apache.maven.api.services.model;

import java.nio.file.Path;

import org.apache.maven.api.Service;

/**
 * Detects whether a given directory is a root directory.
 *
 * @since 4.0.0
 */
public interface RootDetector extends Service {
    /**
     * Checks whether the given directory is a project root directory.
     *
     * @param dir the directory to check
     * @return {@code true} if the directory is a root directory, {@code false} otherwise
     */
    boolean isRootDirectory(Path dir);
}
