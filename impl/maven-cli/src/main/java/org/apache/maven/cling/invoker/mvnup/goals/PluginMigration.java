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
 * Plugin migration configuration for Maven 4 compatibility.
 * This record holds information about plugins that need to be replaced
 * by a different artifact (different groupId and/or artifactId) to work
 * properly with Maven 4.
 *
 * @param oldGroupId the Maven groupId of the old plugin to migrate from
 * @param oldArtifactId the Maven artifactId of the old plugin to migrate from
 * @param newGroupId the Maven groupId of the new plugin to migrate to
 * @param newArtifactId the Maven artifactId of the new plugin to migrate to
 * @param minVersion the minimum version of the new plugin required for Maven 4 compatibility
 * @param reason the reason why this plugin needs to be migrated
 */
public record PluginMigration(
        String oldGroupId,
        String oldArtifactId,
        String newGroupId,
        String newArtifactId,
        String minVersion,
        String reason) {}
