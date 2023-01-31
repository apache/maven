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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.building.ModelCache;

/**
 * A simple model cache used to accelerate model building during a reactor build.
 *
 * @author Benjamin Bentmann
 */
class ReactorModelCache implements ModelCache {

    private final Map<CacheKey, Object> models = new ConcurrentHashMap<>(256);

    public Object get(String groupId, String artifactId, String version, String tag) {
        return models.get(new CacheKey(groupId, artifactId, version, tag));
    }

    public void put(String groupId, String artifactId, String version, String tag, Object data) {
        models.put(new CacheKey(groupId, artifactId, version, tag), data);
    }

    private static final class CacheKey {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String tag;

        private final int hashCode;

        CacheKey(String groupId, String artifactId, String version, String tag) {
            this.groupId = (groupId != null) ? groupId : "";
            this.artifactId = (artifactId != null) ? artifactId : "";
            this.version = (version != null) ? version : "";
            this.tag = (tag != null) ? tag : "";

            int hash = 17;
            hash = hash * 31 + this.groupId.hashCode();
            hash = hash * 31 + this.artifactId.hashCode();
            hash = hash * 31 + this.version.hashCode();
            hash = hash * 31 + this.tag.hashCode();
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

            return artifactId.equals(that.artifactId)
                    && groupId.equals(that.groupId)
                    && version.equals(that.version)
                    && tag.equals(that.tag);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
