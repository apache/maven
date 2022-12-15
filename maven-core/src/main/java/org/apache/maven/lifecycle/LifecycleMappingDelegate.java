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
package org.apache.maven.lifecycle;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.MavenProject;

/**
 * Lifecycle mapping delegate component interface. Calculates project build execution plan given {@link Lifecycle} and
 * lifecycle phase. Standard lifecycles use plugin execution {@code <phase>} or mojo default lifecycle phase to
 * calculate the execution plan, but custom lifecycles can use alternative mapping strategies.
 * <p>
 * Implementations of this interface must be annotated with either {@code @Named("lifecycle-id")} or equivalent plexus
 * {@code @Component} annotations.
 *
 * @since 3.2.0
 * @see org.apache.maven.lifecycle.internal.DefaultLifecycleMappingDelegate
 * @author ifedorenko
 */
public interface LifecycleMappingDelegate {
    Map<String, List<MojoExecution>> calculateLifecycleMappings(
            MavenSession session, MavenProject project, Lifecycle lifecycle, String lifecyclePhase)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException;
}
