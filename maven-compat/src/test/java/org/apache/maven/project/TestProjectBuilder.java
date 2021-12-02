package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.Collections;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.RemoteRepositoryManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named( "classpath" )
@Singleton
public class TestProjectBuilder
    extends DefaultProjectBuilder
{
    @Inject
    public TestProjectBuilder(
            ModelBuilder modelBuilder, ModelProcessor modelProcessor,
            ProjectBuildingHelper projectBuildingHelper, MavenRepositorySystem repositorySystem,
            RepositorySystem repoSystem, RemoteRepositoryManager repositoryManager,
            ProjectDependenciesResolver dependencyResolver, ModelCacheFactory modelCacheFactory )
    {
        super( modelBuilder, modelProcessor, projectBuildingHelper, repositorySystem, repoSystem,
                repositoryManager, dependencyResolver, modelCacheFactory );
    }

    @Override
    public ProjectBuildingResult build( File pomFile, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        ProjectBuildingResult result = super.build( pomFile, configuration );

        result.getProject().setRemoteArtifactRepositories( Collections.<ArtifactRepository> emptyList() );

        return result;
    }

}