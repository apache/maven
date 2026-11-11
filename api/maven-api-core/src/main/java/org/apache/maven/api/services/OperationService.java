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
package org.apache.maven.api.services;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Marker interface for observable Maven services that execute operations within
 * the Maven operation lifecycle.
 * <p>
 * Unlike utility services (such as factories, parsers, interpolators, lookups, and registries)
 * which perform lightweight, stateless, or high-frequency in-memory transformations and remain
 * plain {@link Service} instances, operation services represent substantial, observable units of
 * execution that:
 * <ul>
 *   <li>Follow a structured {@link Request} and {@link Result} pattern capturing operation inputs,
 *       context, and outcomes.</li>
 *   <li>Participate in hierarchical execution diagnostics and call graphs via {@link RequestTrace}.</li>
 *   <li>Integrate with session-level caching through {@link org.apache.maven.api.cache.RequestCache}
 *       to avoid redundant execution across requests or builds.</li>
 *   <li>Provide the foundation for lifecycle event dispatching (such as operation start, progress,
 *       and completion/failure events) across the operational pipeline.</li>
 * </ul>
 *
 * @since 4.1.0
 * @see Service
 * @see Request
 * @see Result
 * @see RequestTrace
 * @see org.apache.maven.api.cache.RequestCache
 */
@Experimental
@ThreadSafe
public interface OperationService extends Service {}
