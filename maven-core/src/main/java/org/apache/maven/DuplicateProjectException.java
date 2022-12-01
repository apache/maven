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
package org.apache.maven;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Signals a collision of two or more projects with the same g:a:v during a reactor build.
 *
 * @author Benjamin Bentmann
 */
public class DuplicateProjectException extends MavenExecutionException {

    private Map<String, List<File>> collisions;

    /**
     * Creates a new exception with specified details.
     *
     * @param message The message text, may be {@code null}.
     * @param collisions The POM files of the projects that collided, indexed by their g:a:v, may be {@code null}.
     */
    public DuplicateProjectException(String message, Map<String, List<File>> collisions) {
        super(message, (File) null);

        this.collisions = (collisions != null) ? collisions : new LinkedHashMap<String, List<File>>();
    }

    /**
     * Gets the POM files of the projects that collided.
     *
     * @return The POM files of the projects that collided, indexed by their g:a:v, never {@code null}.
     */
    public Map<String, List<File>> getCollisions() {
        return collisions;
    }
}
