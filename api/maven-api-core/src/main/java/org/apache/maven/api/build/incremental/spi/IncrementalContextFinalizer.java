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
package org.apache.maven.api.build.incremental.spi;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Callback interface for registering build contexts that should be committed
 * at the end of a mojo execution.
 *
 * <p>The Maven runtime implements this interface to collect all
 * {@link CommittableIncrementalContext} instances created during a mojo's execution and
 * commit them in a single batch after the mojo completes. This ensures that:</p>
 * <ul>
 *   <li>State is persisted only after the mojo succeeds (no partial state on failure)</li>
 *   <li>Stale outputs from all contexts are cleaned up together</li>
 *   <li>Stale output cleanup happens atomically</li>
 * </ul>
 *
 * <p>A mojo that uses multiple {@link org.apache.maven.api.build.incremental.IncrementalContext}
 * instances (e.g., for different source roots) will have each context registered
 * individually through this finalizer.</p>
 *
 * @since 4.1.0
 * @see CommittableIncrementalContext#commit()
 * @see IncrementalContextEnvironment#getFinalizer()
 */
@Experimental
@ThreadSafe
@Consumer
public interface IncrementalContextFinalizer {

    /**
     * Registers a build context to be committed when the mojo execution completes.
     *
     * @param context the build context to register
     */
    void registerContext(@Nonnull CommittableIncrementalContext context);
}
