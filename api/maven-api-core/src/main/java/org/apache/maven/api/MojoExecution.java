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

import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.xml.XmlNode;

/**
 * A {@code MojoExecution} represents a single execution of a Maven Plugin during a given build.
 * An instance of this object is bound to the {@link org.apache.maven.api.di.MojoExecutionScoped}
 * and available as {@code mojoExecution} within {@link org.apache.maven.api.plugin.annotations.Parameter}
 * expressions.
 * <p>
 * Instances are immutable snapshots taken at the point when execution begins (after configuration
 * merging, descriptor resolution, and lifecycle phase assignment are all complete).
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface MojoExecution {

    /** {@return the plugin that owns this execution} */
    @Nonnull
    Plugin plugin();

    /**
     * {@return the {@code <execution>} element from the POM (or from the default lifecycle bindings)
     * that corresponds to this execution, or empty for a direct CLI invocation
     * (e.g. {@code mvn groupId:artifactId:goal})}
     * <p>
     * Default lifecycle bindings (such as {@code maven-compiler-plugin:compile} bound to the
     * {@code compile} phase for {@code jar} packaging) are synthesised from lifecycle mapping
     * metadata and injected as {@code <execution>} elements before model resolution, so they
     * are also present here.  Only a CLI-invoked goal — which has no backing {@code <plugin>}
     * entry in the model — results in an empty {@code Optional}.
     */
    @Nonnull
    Optional<PluginExecution> model();

    /** {@return the descriptor of the mojo being executed} */
    @Nonnull
    MojoDescriptor descriptor();

    /** {@return the execution identifier as declared in the POM} */
    @Nonnull
    String executionId();

    /** {@return the goal being executed} */
    @Nonnull
    String goal();

    /** {@return the lifecycle phase this execution is bound to, or empty for a direct CLI invocation} */
    @Nonnull
    Optional<String> lifecyclePhase();

    /** {@return the merged configuration for this execution, if any} */
    @Nonnull
    Optional<XmlNode> configuration();

    // -------------------------------------------------------------------------
    // Deprecated get-prefixed accessors — use noun-based methods instead
    // -------------------------------------------------------------------------

    /**
     * @deprecated Use {@link #plugin()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Plugin getPlugin() {
        return plugin();
    }

    /**
     * @deprecated Use {@link #model()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nullable
    default PluginExecution getModel() {
        return model().orElse(null);
    }

    /**
     * @deprecated Use {@link #descriptor()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default MojoDescriptor getDescriptor() {
        return descriptor();
    }

    /**
     * @deprecated Use {@link #executionId()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default String getExecutionId() {
        return executionId();
    }

    /**
     * @deprecated Use {@link #goal()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default String getGoal() {
        return goal();
    }

    /**
     * @deprecated Use {@link #lifecyclePhase()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nullable
    default String getLifecyclePhase() {
        return lifecyclePhase().orElse(null);
    }

    /**
     * @deprecated Use {@link #configuration()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Optional<XmlNode> getConfiguration() {
        return configuration();
    }
}
