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
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;

/**
 * Represents a maven plugin runtime
 *
 * @since 4.0.0
 */
@Experimental
public interface Plugin {

    /**
     * Returns the POM model element for this plugin.
     *
     * @return the plugin model, or {@code null} if not available
     */
    @Nullable
    org.apache.maven.api.model.Plugin getModel();

    /**
     * Returns the plugin descriptor containing metadata about goals and parameters.
     *
     * @return the plugin descriptor, never {@code null}
     */
    @Nonnull
    PluginDescriptor getDescriptor();

    /**
     * Returns the custom lifecycles defined by this plugin.
     *
     * @return the list of lifecycles, never {@code null}
     */
    @Nonnull
    List<Lifecycle> getLifecycles();

    /**
     * Returns the class loader used to load this plugin's classes.
     *
     * @return the class loader, never {@code null}
     */
    @Nonnull
    ClassLoader getClassLoader();

    /**
     * Returns the artifact for this plugin.
     *
     * @return the plugin artifact, or {@code null} if not available
     */
    @Nullable
    Artifact getArtifact();

    /**
     * Returns the collection of dependencies for this plugin.
     *
     * @return the plugin dependencies, never {@code null}
     */
    @Nonnull
    default Collection<Dependency> getDependencies() {
        return getDependenciesMap().values();
    }

    /**
     * Returns the dependencies for this plugin, keyed by their string identifier.
     *
     * @return the dependency map, never {@code null}
     */
    @Nonnull
    Map<String, Dependency> getDependenciesMap();
}
