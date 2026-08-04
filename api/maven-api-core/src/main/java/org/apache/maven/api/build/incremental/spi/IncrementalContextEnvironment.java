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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Provides the environment configuration needed to initialize a build context,
 * including state file location, workspace, parameters, and an optional finalizer.
 *
 * <p>This interface is implemented by the Maven runtime (or IDE integration) and
 * passed to the build context constructor. It bundles everything the context needs
 * to initialize:</p>
 * <ul>
 *   <li><strong>State file</strong> — where to persist input/output relationships between
 *       builds. Typically located under {@code target/} so that a clean build starts fresh.</li>
 *   <li><strong>Workspace</strong> — the file system abstraction (see {@link Workspace}).</li>
 *   <li><strong>Parameters</strong> — mojo configuration values. The build context compares
 *       these against the previous build; if any change, all inputs are treated as modified.</li>
 *   <li><strong>Finalizer</strong> — optional callback that commits the context after
 *       mojo execution (see {@link IncrementalContextFinalizer}).</li>
 * </ul>
 *
 * @since 4.1.0
 * @see Workspace
 * @see IncrementalContextFinalizer
 * @see CommittableIncrementalContext
 */
@Experimental
@ThreadSafe
@Consumer
public interface IncrementalContextEnvironment {

    /**
     * {@return the path to the file where build context state is persisted}
     */
    @Nonnull
    Path getStateFile();

    /**
     * {@return the workspace that provides file system abstraction}
     */
    @Nonnull
    Workspace getWorkspace();

    /**
     * Returns the configuration parameters for this build context.
     *
     * <p>These parameters represent <strong>non-file state</strong> that affects the build
     * output — mojo configuration values, dependency digests, and any other context that
     * determines what the mojo will produce. The build context compares these values against
     * the previous build's stored parameters; if <em>any</em> value has changed, all inputs
     * are treated as modified (<em>escalation</em>), forcing a full rebuild.</p>
     *
     * <p>In the Maven runtime, this map is populated automatically by a
     * {@code MojoConfigurationDigester} that:</p>
     * <ul>
     *   <li>Reflects on every {@code @Parameter}-annotated field of the mojo class,
     *       evaluates its expression, and computes a digest of the resolved value</li>
     *   <li>Computes a SHA-1 digest of the plugin's classpath JARs (by content,
     *       not by timestamp, so rebuilding unchanged sources does not trigger
     *       false escalation)</li>
     * </ul>
     *
     * <p>This means mojos automatically get correct incremental behavior when their
     * configuration changes — compiler flags, output directories, filter tokens,
     * plugin dependency versions — without any extra code. The mojo only needs to
     * register its file inputs and associate outputs; the build context handles the rest.</p>
     *
     * @return the configuration parameters, never {@code null}
     */
    @Nonnull
    Map<String, Serializable> getParameters();

    /**
     * {@return the optional context finalizer, or {@code null} if none is configured}
     */
    @Nullable
    IncrementalContextFinalizer getFinalizer();
}
