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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.maven.building.Source;
import org.apache.maven.model.building.ModelCache;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystemSession;

import static java.util.Objects.requireNonNull;

/**
 * A model builder cache backed by the repository system cache.
 *
 */
public class DefaultModelCache implements ModelCache {
    private static final String KEY = DefaultModelCache.class.getName();

    @SuppressWarnings("unchecked")
    public static ModelCache newInstance(RepositorySystemSession session) {
        ConcurrentHashMap<Object, Supplier<?>> cache;
        RepositoryCache repositoryCache = session.getCache();
        if (repositoryCache == null) {
            cache = new ConcurrentHashMap<>();
        } else {
            cache = (ConcurrentHashMap<Object, Supplier<?>>)
                    repositoryCache.computeIfAbsent(session, KEY, ConcurrentHashMap::new);
        }
        return new DefaultModelCache(cache);
    }

    private final ConcurrentMap<Object, Supplier<?>> cache;

    private DefaultModelCache(ConcurrentMap<Object, Supplier<?>> cache) {
        this.cache = requireNonNull(cache);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T computeIfAbsent(String groupId, String artifactId, String version, String tag, Supplier<T> data) {
        return (T) computeIfAbsent(new GavCacheKey(groupId, artifactId, version, tag), data);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T computeIfAbsent(Source path, String tag, Supplier<T> data) {
        return (T) computeIfAbsent(new SourceCacheKey(path, tag), data);
    }

    protected Object computeIfAbsent(Object key, Supplier<?> data) {
        return cache.computeIfAbsent(key, k -> new CachingSupplier<>(data)).get();
    }

    static class GavCacheKey {

        private final String gav;

        private final String tag;

        private final int hash;

        GavCacheKey(String groupId, String artifactId, String version, String tag) {
            this(gav(groupId, artifactId, version), tag);
        }

        GavCacheKey(String gav, String tag) {
            this.gav = gav;
            this.tag = tag;
            this.hash = Objects.hash(gav, tag);
        }

        private static String gav(String groupId, String artifactId, String version) {
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
            GavCacheKey that = (GavCacheKey) obj;
            return Objects.equals(this.gav, that.gav) && Objects.equals(this.tag, that.tag);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "GavCacheKey{" + "gav='" + gav + '\'' + ", tag='" + tag + '\'' + '}';
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
            return "SourceCacheKey{" + "source=" + source + ", tag='" + tag + '\'' + '}';
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
            if (v instanceof AltRes) {
                uncheckedThrow(((AltRes) v).t);
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
