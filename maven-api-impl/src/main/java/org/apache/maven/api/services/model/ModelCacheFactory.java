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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Factory for creating model caches.
 * <p>
 * The model cache is meant for exclusive consumption by the model builder and is opaque to the cache implementation.
 * The cache is created once per session and is valid through the lifetime of the session.
 * <p>
 * The cache implementation could be annotated with {@code SessionScoped} to be created once per session, but
 * this would make tests more complicated to write as they would all need to enter the session scope.
 * This is similar to the {@code CIFriendlyVersionModelTransformer}.
 *
 * @since 4.0.0
 */
@Experimental
public interface ModelCacheFactory {

    @Nonnull
    ModelCache newInstance();
}
