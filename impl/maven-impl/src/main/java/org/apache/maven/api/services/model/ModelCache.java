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

import java.util.List;
import java.util.function.Supplier;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.services.Source;

/**
 * Caches auxiliary data used during model building like already processed raw/effective models. The data in the cache
 * is meant for exclusive consumption by the model builder and is opaque to the cache implementation. The cache key is
 * formed by a combination of group id, artifact id, version and tag, or by the pom path on the filesystem and tag.
 * The tag allows for further classification of the associated data on the sole discretion of the model builder.
 * The cache is expected to be valid through the lifetime of the session, so the model builder is not allowed to
 * store data which may change during the session, especially effective models which may be different if the
 * user properties or activate profiles change between two invocations of the model builder.
 * The cache implementation is expected to be thread-safe.
 *
 * @since 4.0.0
 */
@Experimental
@ThreadSafe
public interface ModelCache {

    <T> T computeIfAbsent(
            List<RemoteRepository> repositories,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String tag,
            Supplier<T> data);

    <T> T computeIfAbsent(Source path, String tag, Supplier<T> data);

    void clear();
}
