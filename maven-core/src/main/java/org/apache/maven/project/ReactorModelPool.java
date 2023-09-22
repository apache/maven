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

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Model;

/**
 * Holds all Models that are known to the reactor. This allows the project builder to resolve imported Models from the
 * reactor when building another project's effective model.
 *
 */
class ReactorModelPool {
    private final Map<GAKey, Set<Model>> modelsByGa = new ConcurrentHashMap<>();

    private final Map<Path, Model> modelsByPath = new ConcurrentHashMap<>();

    /**
     * Get the model by its GAV or (since 4.0.0) by its GA if there is only one.
     *
     * @param groupId never {@code null}
     * @param artifactId never {@code null}
     * @param version can be {@code null}
     * @return the matching model or {@code null}
     * @throws IllegalStateException if version was null and multiple modules share the same groupId + artifactId
     */
    public Model get(String groupId, String artifactId, String version) {
        return modelsByGa.getOrDefault(new GAKey(groupId, artifactId), Collections.emptySet()).stream()
                .filter(m -> version == null || version.equals(getVersion(m)))
                .reduce((a, b) -> {
                    throw new IllegalStateException(
                            "Multiple modules with key " + a.getGroupId() + ':' + a.getArtifactId());
                })
                .orElse(null);
    }

    private String getVersion(Model model) {
        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }
        return version;
    }

    void put(Path pomFile, Model model) {
        modelsByPath.put(pomFile, model);
        modelsByGa
                .computeIfAbsent(new GAKey(getGroupId(model), model.getArtifactId()), k -> new HashSet<>())
                .add(model);
    }

    private static String getGroupId(Model model) {
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    private static final class GAKey {

        private final String groupId;

        private final String artifactId;

        private final int hashCode;

        GAKey(String groupId, String artifactId) {
            this.groupId = (groupId != null) ? groupId : "";
            this.artifactId = (artifactId != null) ? artifactId : "";

            hashCode = Objects.hash(this.groupId, this.artifactId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GAKey)) {
                return false;
            }
            GAKey that = (GAKey) obj;
            return artifactId.equals(that.artifactId) && groupId.equals(that.groupId);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return groupId + ':' + artifactId;
        }
    }
}
