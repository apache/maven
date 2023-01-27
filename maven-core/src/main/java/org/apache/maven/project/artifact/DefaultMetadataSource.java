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
package org.apache.maven.project.artifact;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.ProjectBuilder;

/**
 * This realizes the metadata source via the default hint to provide backward-compat with Maven 2.x whose Plexus version
 * registered component descriptors twice: once keyed by role+roleHint and once keyed by role only. This effectively
 * made the metadata source available with its original role hint ("maven") as well as the default hint.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultMetadataSource extends MavenMetadataSource {
    @Inject
    public DefaultMetadataSource(
            RepositoryMetadataManager repositoryMetadataManager,
            ArtifactFactory repositorySystem,
            ProjectBuilder projectBuilder,
            MavenMetadataCache cache,
            LegacySupport legacySupport) {
        super(repositoryMetadataManager, repositorySystem, projectBuilder, cache, legacySupport);
    }
}
