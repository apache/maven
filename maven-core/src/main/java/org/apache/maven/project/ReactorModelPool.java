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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds all POM files that are known to the reactor. This allows the project builder to resolve imported POMs from the
 * reactor when building another project's effective model.
 *
 * @author Benjamin Bentmann
 */
class ReactorModelPool {

    private final Map<CacheKey, File> pomFiles = new HashMap<>();

    public File get(String groupId, String artifactId, String version) {
        return pomFiles.get(new CacheKey(groupId, artifactId, version));
    }

    public void put(String groupId, String artifactId, String version, File pomFile) {
        pomFiles.put(new CacheKey(groupId, artifactId, version), pomFile);
    }

    private static final class CacheKey {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final int hashCode;

        CacheKey(String groupId, String artifactId, String version) {
            this.groupId = (groupId != null) ? groupId : "";
            this.artifactId = (artifactId != null) ? artifactId : "";
            this.version = (version != null) ? version : "";

            int hash = 17;
            hash = hash * 31 + this.groupId.hashCode();
            hash = hash * 31 + this.artifactId.hashCode();
            hash = hash * 31 + this.version.hashCode();
            hashCode = hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof CacheKey)) {
                return false;
            }

            CacheKey that = (CacheKey) obj;

            return artifactId.equals(that.artifactId) && groupId.equals(that.groupId) && version.equals(that.version);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(groupId).append(':').append(artifactId).append(':').append(version);
            return buffer.toString();
        }
    }
}
