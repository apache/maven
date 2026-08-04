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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Provider;
import org.apache.maven.api.build.incremental.IncrementalContext;

/**
 * Extended {@link IncrementalContext} that supports committing state changes.
 *
 * <p>This interface is implemented by the build context implementation, not by mojos.
 * The Maven runtime (or IDE integration) uses it to commit all state changes at the
 * end of a mojo execution, typically through a {@link IncrementalContextFinalizer}:</p>
 * <ol>
 *   <li>The mojo registers inputs, creates outputs via the {@link IncrementalContext} API</li>
 *   <li>The finalizer calls {@link #commit()} which persists the new state and
 *       deletes stale outputs</li>
 * </ol>
 *
 * @since 4.1.0
 * @see IncrementalContextFinalizer
 */
@Experimental
@NotThreadSafe
@Provider
public interface CommittableIncrementalContext extends IncrementalContext {

    /**
     * Commits all changes in this build context — persists state and deletes stale outputs.
     */
    void commit();
}
