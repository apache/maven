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
 *
 * @since 4.0.0
 */
@Experimental
public interface MojoExecution {

    /** {@return the plugin that owns this execution} */
    @Nonnull
    Plugin plugin();

    /**
     * @deprecated Use {@link #plugin()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default Plugin getPlugin() {
        return plugin();
    }

    /** {@return the execution model element, if any} */
    @Nonnull
    Optional<PluginExecution> model();

    /**
     * @deprecated Use {@link #model()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default PluginExecution getModel() {
        return model().orElse(null);
    }

    /** {@return the descriptor of the mojo being executed} */
    @Nonnull
    MojoDescriptor descriptor();

    /**
     * @deprecated Use {@link #descriptor()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default MojoDescriptor getDescriptor() {
        return descriptor();
    }

    /** {@return the execution identifier, or empty for a direct CLI invocation} */
    @Nonnull
    Optional<String> executionId();

    /**
     * @deprecated Use {@link #executionId()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default String getExecutionId() {
        return executionId().orElse(null);
    }

    /** {@return the goal being executed} */
    @Nonnull
    String goal();

    /**
     * @deprecated Use {@link #goal()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default String getGoal() {
        return goal();
    }

    /** {@return the lifecycle phase this execution is bound to, or empty for a direct CLI invocation} */
    @Nonnull
    Optional<String> lifecyclePhase();

    /**
     * @deprecated Use {@link #lifecyclePhase()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default String getLifecyclePhase() {
        return lifecyclePhase().orElse(null);
    }

    /** {@return the merged configuration for this execution, if any} */
    @Nonnull
    Optional<XmlNode> configuration();

    /**
     * @deprecated Use {@link #configuration()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default Optional<XmlNode> getConfiguration() {
        return configuration();
    }

    /**
     * {@return true if this mojo triggers a forked lifecycle execution}
     *
     * @since 4.2.0
     */
    default boolean isForkingExecution() {
        MojoDescriptor d = descriptor();
        return (d.getExecutePhase() != null && !d.getExecutePhase().isEmpty())
                || (d.getExecuteGoal() != null && !d.getExecuteGoal().isEmpty());
    }
}
