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
 * @param minVersion the minimum version required for Maven 4 compatibility
 * @param reason the reason why this plugin needs to be upgraded
 */
public record PluginUpgrade(String groupId, String artifactId, String minVersion, String reason) {}
