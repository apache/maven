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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;

/**
 * Represents a loaded Maven plugin runtime instance.
 * <p>
 * A {@code Plugin} provides access to the plugin's POM {@linkplain org.apache.maven.api.model.Plugin model},
 * its {@link PluginDescriptor descriptor}, custom build {@linkplain Lifecycle lifecycles}, isolated
 * {@link ClassLoader}, main {@link Artifact}, and runtime plugin {@linkplain Dependency dependencies}.
 * </p>
 *
 * @since 4.0.0
 * @see MojoExecution#getPlugin()
 */
@Experimental
public interface Plugin {

    /**
     * Returns the POM model representation of this plugin.
     *
     * @return the plugin model from the POM, never {@code null}
     */
    @Nonnull
    org.apache.maven.api.model.Plugin getModel();

    /**
     * Returns the plugin descriptor containing metadata about mojos, parameters, and requirements.
     *
     * @return the plugin descriptor, never {@code null}
     */
    @Nonnull
    PluginDescriptor getDescriptor();

    /**
     * Returns the custom build lifecycles defined by this plugin, if any.
     *
     * @return an unmodifiable list of custom {@link Lifecycle} definitions, never {@code null}
     */
    @Nonnull
    List<Lifecycle> getLifecycles();

    /**
     * Returns the {@link ClassLoader} used to load this plugin and its dependencies.
     *
     * @return the plugin class loader, never {@code null}
     */
    @Nonnull
    ClassLoader getClassLoader();

    /**
     * Returns the primary {@link Artifact} of this plugin.
     *
     * @return the plugin artifact, never {@code null}
     */
    @Nonnull
    Artifact getArtifact();

    /**
     * Returns a collection of direct and transitive dependencies required by this plugin.
     *
     * @return a collection of {@link Dependency} instances, never {@code null}
     */
    @Nonnull
    default Collection<Dependency> getDependencies() {
        return getDependenciesMap().values();
    }

    /**
     * Returns a mapping of dependency keys to dependencies for this plugin.
     *
     * @return an unmodifiable map of dependency key strings to {@link Dependency} instances, never {@code null}
     */
    @Nonnull
    Map<String, Dependency> getDependenciesMap();
}
