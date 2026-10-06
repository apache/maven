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

    /**
     * Returns the runtime {@link Plugin} associated with this execution.
     *
     * @return the plugin runtime, never {@code null}
     */
    @Nonnull
    Plugin getPlugin();

    /**
     * Returns the POM model {@link PluginExecution} corresponding to this execution.
     *
     * @return the plugin execution model, never {@code null}
     */
    @Nonnull
    PluginExecution getModel();

    /**
     * Returns the {@link MojoDescriptor} describing the mojo to be executed.
     *
     * @return the mojo descriptor, never {@code null}
     */
    @Nonnull
    MojoDescriptor getDescriptor();

    /**
     * Returns the unique execution identifier within the plugin configuration.
     *
     * @return the execution ID, never {@code null}
     */
    @Nonnull
    String getExecutionId();

    /**
     * Returns the goal name executed by this mojo.
     *
     * @return the mojo goal, never {@code null}
     */
    @Nonnull
    String getGoal();

    /**
     * Returns the build lifecycle phase to which this execution is bound.
     *
     * @return the lifecycle phase name, or an empty string if not bound to a phase, never {@code null}
     */
    @Nonnull
    String getLifecyclePhase();

    /**
     * Returns the effective XML configuration for this mojo execution, if present.
     *
     * @return an {@link Optional} containing the XML configuration node, or empty if none configured
     */
    @Nonnull
    Optional<XmlNode> getConfiguration();
}
