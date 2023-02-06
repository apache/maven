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

import java.util.Set;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * ArtifactFilterManager
 */
public interface ArtifactFilterManager {
    /**
     * Returns a filter for core + extension artifacts.
     *
     * @deprecated use {@code META-INF/maven/extension.xml} to define artifacts exported by Maven core and plugin
     *             extensions.
     */
    ArtifactFilter getArtifactFilter();

    /**
     * Returns a filter for only the core artifacts.
     */
    ArtifactFilter getCoreArtifactFilter();

    /**
     * Exclude an extension artifact (doesn't affect getArtifactFilter's result, only getExtensionArtifactFilter).
     *
     * @deprecated use {@code META-INF/maven/extension.xml} to define artifacts exported by Maven core and plugin
     *             extensions.
     */
    void excludeArtifact(String artifactId);

    Set<String> getCoreArtifactExcludes();
}
