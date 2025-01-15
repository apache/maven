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
package org.apache.maven.internal.impl.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.model.ModelCache;

import static java.util.Objects.requireNonNull;

/**
 * A model builder cache backed by the repository system cache.
 *
 */
public class DefaultModelCache implements ModelCache {

    private final ConcurrentMap<Object, Supplier<?>> cache;

    public DefaultModelCache() {
        this(new ConcurrentHashMap<>());
    }

    private DefaultModelCache(ConcurrentMap<Object, Supplier<?>> cache) {
        this.cache = requireNonNull(cache);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T computeIfAbsent(
            List<RemoteRepository> repositories,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String tag,
            Supplier<T> data) {
        return (T) computeIfAbsent(new RgavCacheKey(repositories, groupId, artifactId, version, classifier, tag), data);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T computeIfAbsent(Source path, String tag, Supplier<T> data) {
        return (T) computeIfAbsent(new SourceCacheKey(path, tag), data);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    protected Object computeIfAbsent(Object key, Supplier<?> data) {
        return cache.computeIfAbsent(key, k -> new CachingSupplier<>(data)).get();
    }

    static class RgavCacheKey {

        private final List<RemoteRepository> repositories;

        private final String gav;

        private final String tag;

        private final int hash;

        RgavCacheKey(
                List<RemoteRepository> repositories,
                String groupId,
                String artifactId,
                String version,
                String classifier,
                String tag) {
            this(repositories, gav(groupId, artifactId, version, classifier), tag);
        }

        RgavCacheKey(List<RemoteRepository> repositories, String gav, String tag) {
            this.repositories = List.copyOf(repositories);
            this.gav = gav;
            this.tag = tag;
            this.hash = Objects.hash(this.repositories, this.gav, this.tag);
        }

        private static String gav(String groupId, String artifactId, String version, String classifier) {
            StringBuilder sb = new StringBuilder();
            if (groupId != null) {
                sb.append(groupId);
            }
            sb.append(":");
            if (artifactId != null) {
                sb.append(artifactId);
            }
            sb.append(":");
            if (version != null) {
                sb.append(version);
            }
            sb.append(":");
            if (classifier != null) {
                sb.append(classifier);
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (null == obj || !getClass().equals(obj.getClass())) {
                return false;
            }
            RgavCacheKey that = (RgavCacheKey) obj;
            return Objects.equals(this.repositories, that.repositories)
                    && Objects.equals(this.gav, that.gav)
                    && Objects.equals(this.tag, that.tag);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "GavCacheKey[" + "gav='" + gav + '\'' + ", tag='" + tag + '\'' + ']';
        }
    }

    private static final class SourceCacheKey {
        private final Source source;

        private final String tag;

        private final int hash;

        SourceCacheKey(Source source, String tag) {
            this.source = source;
            this.tag = tag;
            this.hash = Objects.hash(source, tag);
        }

        @Override
        public String toString() {
            return "SourceCacheKey[" + "location=" + source.getLocation() + ", tag=" + tag + ", path="
                    + source.getPath() + ']';
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (null == obj || !getClass().equals(obj.getClass())) {
                return false;
            }
            SourceCacheKey that = (SourceCacheKey) obj;
            return Objects.equals(this.source, that.source) && Objects.equals(this.tag, that.tag);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    static class CachingSupplier<T> implements Supplier<T> {
        final Supplier<T> supplier;
        volatile Object value;

        CachingSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        @SuppressWarnings({"unchecked", "checkstyle:InnerAssignment"})
        public T get() {
            Object v;
            if ((v = value) == null) {
                synchronized (this) {
                    if ((v = value) == null) {
                        try {
                            v = value = supplier.get();
                        } catch (Exception e) {
                            v = value = new AltRes(e);
                        }
                    }
                }
            }
            if (v instanceof AltRes altRes) {
                uncheckedThrow(altRes.t);
            }
            return (T) v;
        }

        static class AltRes {
            final Throwable t;

            AltRes(Throwable t) {
                this.t = t;
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t; // rely on vacuous cast
    }
}
