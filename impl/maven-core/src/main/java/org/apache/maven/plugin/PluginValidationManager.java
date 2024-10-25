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
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;

/**
 * Component collecting plugin validation issues and reporting them.
 *
 * @since 3.9.2
 */
public interface PluginValidationManager {
    enum IssueLocality {
        /**
         * Issue is "user actionable", is internal to the currently built project and is reparable from scope of it
         * by doing some change (for example by changing POM and fixing the problematic plugin configuration).
         */
        INTERNAL,

        /**
         * Issue (present in some plugin) is "developer actionable" (of given plugin, by changing code and doing
         * new release), is NOT local to the currently built project. It may be reparable by updating given plugin
         * to new fixed version, or by dropping plugin use from currently built project.
         * <p>
         * Note: if a reactor build contains a plugin (with issues) and later that built plugin is used in build,
         * it will be reported as "external". It is up to developer to correctly interpret output (GAV) of issues
         * and realize that in this case he wears two hats:" "user" and "(plugin) developer".
         */
        EXTERNAL
    }

    /**
     * Reports plugin issues applicable to the plugin as a whole.
     * <p>
     * This method should be used in "early" phase of plugin execution, possibly even when plugin or mojo descriptor
     * does not exist yet. In turn, this method will not record extra information like plugin occurrence or declaration
     * location as those are not yet available.
     */
    void reportPluginValidationIssue(
            IssueLocality locality, RepositorySystemSession session, Artifact pluginArtifact, String issue);

    /**
     * Reports plugin issues applicable to the plugin as a whole.
     * <p>
     * This method will record extra information as well, like plugin occurrence or declaration location.
     */
    void reportPluginValidationIssue(
            IssueLocality locality, MavenSession mavenSession, MojoDescriptor mojoDescriptor, String issue);

    /**
     * Reports plugin Mojo issues applicable to the Mojo itself.
     * <p>
     * This method will record extra information as well, like plugin occurrence or declaration location.
     */
    void reportPluginMojoValidationIssue(
            IssueLocality locality,
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            String issue);
}
