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

import java.util.Map;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Provides access to the configured parameters of a Mojo without instantiating the root Mojo, invoking its setters,
 * or running its {@code execute} method. Resolving parameters may require resolving the plugin and setting up its
 * class realm. Converting nested configuration may instantiate nested parameter objects and invoke their setters.
 *
 * @since 4.2.0
 */
@Experimental
public interface MojoConfigurationService extends Service {

    /**
     * Resolves the configured parameters of a Mojo execution.
     *
     * Mojos using both the Maven 3 and Maven 4 APIs are supported.
     * Plugin resolution may occur if the plugin realm has not already been set up.
     *
     * The root Mojo is not instantiated, its setters are not invoked, and its
     * {@code execute} method is not run. Plugin-specific parameter configurations are represented as
     * {@code XmlNode} values rather than instantiated as plugin objects. Values supplied through separate injection
     * mechanisms such as {@code @Resolution} are not included.
     *
     * The returned values represent configured parameters and descriptor defaults,
     * not necessarily the complete runtime state of an initialized Mojo.
     *
     * The current implementation accepts Maven's {@code DefaultProject} and
     * {@code DefaultMojoExecution} implementations; other implementations are rejected.
     *
     * Parameter expressions are resolved using the supplied project's context and
     * effective properties.
     *
     * The plugin realm's class loader is temporarily set as the thread context
     * class loader while resolving parameters and is restored afterward.
     *
     * @param project the project associated with the Mojo execution
     * @param mojoExecution the Mojo execution whose parameters are to be resolved
     * @return a map of canonical parameter names to their resolved values;
     *         values are either type-converted Java values or {@code XmlNode}
     *         representations of plugin-specific configuration;
     *         parameters without configured or default values are omitted
     */
    @Nonnull
    Map<String, Object> getParameters(@Nonnull Project project, @Nonnull MojoExecution mojoExecution);
}
