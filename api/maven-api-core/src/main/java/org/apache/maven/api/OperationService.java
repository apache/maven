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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;

/**
 * Marker interface for services whose operations participate in the shared request pipeline.
 * Implementations must be thread-safe: their service methods may be called concurrently
 * from multiple threads.
 * Implementations submit their requests to the session's request pipeline, which provides
 * caching and the context needed to observe operation lifecycles. Requests and results
 * describe the work, while request traces identify the operations that initiated it.
 *
 * <p>All {@link Request} types inherit a {@link org.apache.maven.api.cache.CacheRetention}
 * policy from {@link org.apache.maven.api.cache.CacheMetadata#getCacheRetention()}, which
 * defaults to {@link org.apache.maven.api.cache.CacheRetention#DISABLED}.
 * Request types whose results are safe to cache (i.e. read-only, deterministic operations)
 * must override that default with an appropriate retention policy.</p>
 *
 * <p>Lightweight utilities, factories and leaf operations that do not benefit from the
 * pipeline extend {@link Service} directly.</p>
 *
 * @see Request
 * @see Result
 * @see RequestTrace
 * @since 4.2.0
 */
@Experimental
@ThreadSafe
public interface OperationService extends Service {}
