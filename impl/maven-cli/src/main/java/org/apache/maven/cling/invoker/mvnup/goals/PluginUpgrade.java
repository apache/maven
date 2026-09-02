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
package org.apache.maven.cling.invoker.mvnup.goals;

/**
 * Plugin upgrade configuration for Maven 4 compatibility.
 * This record holds information about plugins that need to be upgraded
 * to specific minimum versions to work properly with Maven 4.
 *
 * @param groupId the Maven groupId of the plugin
 * @param artifactId the Maven artifactId of the plugin
 * @param minVersion the minimum version required for Maven 4 compatibility (for 3.x users)
 * @param latestPreRelease the latest available 4.x pre-release version, or {@code null} if
 *     the plugin has no 4.x pre-release line. Used to upgrade old 4.x alpha/beta/RC versions
 *     to the latest pre-release rather than downgrading to a 3.x version.
 * @param reason the reason why this plugin needs to be upgraded
 * @param minJdk the minimum JDK version required by this plugin version, or {@code 0} if
 *     the plugin works with any JDK. When the project's detected JDK is below this value,
 *     the upgrade is skipped to avoid {@code UnsupportedClassVersionError}.
 */
public record PluginUpgrade(
        String groupId, String artifactId, String minVersion, String latestPreRelease, String reason, int minJdk) {

    /**
     * Convenience constructor for plugins without a 4.x pre-release line and no JDK requirement.
     */
    public PluginUpgrade(String groupId, String artifactId, String minVersion, String reason) {
        this(groupId, artifactId, minVersion, null, reason, 0);
    }

    /**
     * Convenience constructor for plugins with a 4.x pre-release line but no JDK requirement.
     */
    public PluginUpgrade(String groupId, String artifactId, String minVersion, String latestPreRelease, String reason) {
        this(groupId, artifactId, minVersion, latestPreRelease, reason, 0);
    }
}
