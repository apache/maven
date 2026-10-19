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

import org.apache.maven.api.Plugin;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Provides access to plugin runtimes and the services contributed by them.
 *
 * @since 4.1.0
 */
@Experimental
public interface PluginManager extends Service {

    /**
     * Resolves a plugin, creates its classloader when necessary, and returns its runtime metadata.
     * The returned plugin exposes its descriptor, resolved dependencies, artifact, and runtime realm.
     *
     * @param session the session in which to resolve the plugin
     * @param project the project declaring the plugin
     * @param plugin the plugin declaration from the project's model
     * @return the resolved plugin runtime
     * @throws PluginManagerException if the plugin cannot be resolved or loaded
     * @throws NullPointerException if any argument is {@code null}
     */
    @Nonnull
    Plugin loadPlugin(
            @Nonnull Session session, @Nonnull Project project, @Nonnull org.apache.maven.api.model.Plugin plugin);
}
