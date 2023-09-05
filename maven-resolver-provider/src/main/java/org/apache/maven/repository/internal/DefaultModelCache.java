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
package org.apache.maven.repository.internal;

import org.apache.maven.model.building.ModelCache;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystemSession;

/**
 * A model builder cache backed by the repository system cache.
 *
 * @author Benjamin Bentmann
 */
class DefaultModelCache implements ModelCache {

    private final RepositorySystemSession session;

    private final RepositoryCache cache;

    public static ModelCache newInstance(RepositorySystemSession session) {
        if (session.getCache() == null) {
            return null;
        } else {
            return new DefaultModelCache(session);
        }
    }

    private DefaultModelCache(RepositorySystemSession session) {
        this.session = session;
        this.cache = session.getCache();
    }

    @Override
    public Object get(String groupId, String artifactId, String version, String tag) {
        return cache.get(session, new Key(groupId, artifactId, version, tag));
    }

    @Override
    public void put(String groupId, String artifactId, String version, String tag, Object data) {
        cache.put(session, new Key(groupId, artifactId, version, tag), data);
    }

    static class Key {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String tag;

        private final int hash;

        Key(String groupId, String artifactId, String version, String tag) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.tag = tag;

            int h = 17;
            h = h * 31 + this.groupId.hashCode();
            h = h * 31 + this.artifactId.hashCode();
            h = h * 31 + this.version.hashCode();
            h = h * 31 + this.tag.hashCode();
            hash = h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (null == obj || !getClass().equals(obj.getClass())) {
                return false;
            }

            Key that = (Key) obj;
            return artifactId.equals(that.artifactId)
                    && groupId.equals(that.groupId)
                    && version.equals(that.version)
                    && tag.equals(that.tag);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
