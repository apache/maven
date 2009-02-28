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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProfileManager;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Collection;

public interface MavenProjectBuilder
{
    boolean STRICT_MODEL_PARSING = true;

    // Used directly by plugins

    // site
    MavenProject build( File project, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException;

    // remote resources plugin
    MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository, boolean allowStub )
        throws ProjectBuildingException;

    MavenProject build( File project, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException;

    MavenProjectBuildingResult buildProjectWithDependencies( File project, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException;

    MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException;

    MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException;

    PomClassicDomainModel buildModel( File pom,
                                             Collection<InterpolatorProperty> interpolatorProperties,
                                             PomArtifactResolver resolver )
        throws IOException;

    /**
     * Returns a maven project for the specified input stream.
     *
     * @param pom                         input stream of the model
     * @param interpolatorProperties      properties used for interpolation of properties within the model
     * @param resolver                    artifact resolver used in resolving artifacts
     * @param projectBuilderConfiguration
     * @return a maven project for the specified input stream
     * @throws IOException if there is a problem in the construction of the maven project
     */
    MavenProject buildFromLocalPath(File pom,
                                    Collection<InterpolatorProperty> interpolatorProperties,
                                    PomArtifactResolver resolver,
                                    ProjectBuilderConfiguration projectBuilderConfiguration,
                                    MavenProjectBuilder mavenProjectBuilder)
        throws IOException;

    Model getSuperModel();
}
