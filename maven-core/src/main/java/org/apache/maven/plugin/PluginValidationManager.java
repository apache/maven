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
package org.apache.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Component collecting plugin validation issues and reporting them.
 *
 * @since 3.9.2
 */
public interface PluginValidationManager {

    String pluginKey(String groupId, String artifactId, String version);

    String pluginKey(Plugin plugin);

    String pluginKey(MojoDescriptor mojoDescriptor);

    void reportPluginValidationIssue(RepositorySystemSession session, String pluginKey, String issue);

    void reportPluginValidationIssue(MavenSession mavenSession, MojoDescriptor mojoDescriptor, String issue);

    void reportPluginMojoValidationIssue(
            MavenSession mavenSession, MojoDescriptor mojoDescriptor, Class<?> mojoClass, String issue);
}
