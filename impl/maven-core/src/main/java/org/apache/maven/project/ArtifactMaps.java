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
package org.apache.maven.project;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;

/**
 * Builds lookup maps of {@link Artifact} for {@link MavenProject}.
 *
 * @since 4.1.0
 */
final class ArtifactMaps {

    private ArtifactMaps() {}

    /**
     * Builds a map of artifacts keyed by {@link Artifact#getDependencyConflictId()}
     * (groupId:artifactId:type[:classifier]). Unlike {@link MavenProject#getArtifactMap()}, this disambiguates
     * artifacts that share the same groupId and artifactId but differ by classifier, such as a jar and its
     * "tests" or "sources" classified companion, which would otherwise collide under the same key (MNG-5552).
     *
     * @param artifacts the artifacts to build the map from
     * @return a mutable map keyed by dependency conflict id; if two artifacts share the same key the last one wins
     */
    static Map<String, Artifact> byDependencyConflictId(Collection<Artifact> artifacts) {
        Map<String, Artifact> map = new LinkedHashMap<>();
        artifacts.forEach(artifact -> map.put(artifact.getDependencyConflictId(), artifact));
        return map;
    }
}
