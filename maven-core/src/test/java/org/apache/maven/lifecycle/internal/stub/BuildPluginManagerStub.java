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
package org.apache.maven.lifecycle.internal.stub;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * @author Kristian Rosenvold
 */
public class BuildPluginManagerStub implements BuildPluginManager {

    public PluginDescriptor loadPlugin(
            Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session) {
        return null;
    }

    public MojoDescriptor getMojoDescriptor(
            Plugin plugin, String goal, List<RemoteRepository> repositories, RepositorySystemSession session) {
        return MojoExecutorStub.createMojoDescriptor(plugin);
    }

    public ClassRealm getPluginRealm(MavenSession session, PluginDescriptor pluginDescriptor) {
        return null;
    }

    public void executeMojo(MavenSession session, MojoExecution execution) {}
}
