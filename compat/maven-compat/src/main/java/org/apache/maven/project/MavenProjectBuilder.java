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
package org.apache.maven.project;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.wagon.events.TransferListener;

/**
 * @deprecated use {@link ProjectBuilder} instead
 */
@Deprecated
public interface MavenProjectBuilder {

    MavenProject build(File pom, ProjectBuilderConfiguration configuration) throws ProjectBuildingException;

    // TODO maven-site-plugin -- not used by the plugin directly, but used by Doxia Integration Tool & MPIR
    // see DOXIASITETOOLS-167 & MPIR-349
    MavenProject build(File pom, ArtifactRepository localRepository, ProfileManager profileManager)
            throws ProjectBuildingException;

    // TODO remote-resources-plugin
    MavenProject buildFromRepository(
            Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository)
            throws ProjectBuildingException;

    // TODO remote-resources-plugin
    MavenProject buildFromRepository(
            Artifact artifact,
            List<ArtifactRepository> remoteRepositories,
            ArtifactRepository localRepository,
            boolean allowStubModel)
            throws ProjectBuildingException;

    // TODO this is only to provide a project for plugins that don't need a project to execute but need some
    // of the values from a MavenProject. Ideally this should be something internal and nothing outside Maven
    // would ever need this so it should not be exposed in a public API
    MavenProject buildStandaloneSuperProject(ProjectBuilderConfiguration configuration) throws ProjectBuildingException;

    MavenProject buildStandaloneSuperProject(ArtifactRepository localRepository) throws ProjectBuildingException;

    MavenProject buildStandaloneSuperProject(ArtifactRepository localRepository, ProfileManager profileManager)
            throws ProjectBuildingException;

    MavenProject buildWithDependencies(
            File pom,
            ArtifactRepository localRepository,
            ProfileManager globalProfileManager,
            TransferListener transferListener)
            throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException;

    MavenProject buildWithDependencies(
            File pom, ArtifactRepository localRepository, ProfileManager globalProfileManager)
            throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException;
}
