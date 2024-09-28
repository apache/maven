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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.Collections;

import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.model.LifecycleBindingsInjector;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.root.RootLocator;
import org.eclipse.aether.RepositorySystem;

@Named("classpath")
@Singleton
@Deprecated
public class TestProjectBuilder extends DefaultProjectBuilder {
    @Inject
    public TestProjectBuilder(
            ModelBuilder modelBuilder,
            ProjectBuildingHelper projectBuildingHelper,
            MavenRepositorySystem repositorySystem,
            RepositorySystem repoSystem,
            ProjectDependenciesResolver dependencyResolver,
            RootLocator rootLocator,
            LifecycleBindingsInjector lifecycleBindingsInjector) {
        super(
                modelBuilder,
                projectBuildingHelper,
                repositorySystem,
                repoSystem,
                dependencyResolver,
                rootLocator,
                lifecycleBindingsInjector);
    }

    @Override
    public ProjectBuildingResult build(File pomFile, ProjectBuildingRequest configuration)
            throws ProjectBuildingException {
        ProjectBuildingResult result = super.build(pomFile, configuration);

        result.getProject().setRemoteArtifactRepositories(Collections.emptyList());

        return result;
    }
}
