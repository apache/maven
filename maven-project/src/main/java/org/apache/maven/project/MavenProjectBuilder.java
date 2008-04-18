package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

import java.io.File;
import java.util.List;

public interface MavenProjectBuilder
{
    String ROLE = MavenProjectBuilder.class.getName();

    String STANDALONE_SUPERPOM_GROUPID = "org.apache.maven";

    String STANDALONE_SUPERPOM_ARTIFACTID = "super-pom";

    String STANDALONE_SUPERPOM_VERSION = "2.1";

    boolean STRICT_MODEL_PARSING = true;

    /**
     * @deprecated Use {@link MavenProjectBuilder#build(File, ProjectBuilderConfiguration)} instead.
     */
    MavenProject build( File project,
                        ArtifactRepository localRepository,
                        ProfileManager globalProfileManager )
        throws ProjectBuildingException;

    MavenProject build( File project, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException;

    MavenProject buildWithDependencies( File project,
                                        ArtifactRepository localRepository,
                                        ProfileManager globalProfileManager )
        throws ProjectBuildingException;

    MavenProjectBuildingResult buildProjectWithDependencies( File project,
                                                             ProjectBuilderConfiguration config )
        throws ProjectBuildingException;


    /**
     * @deprecated Use {@link MavenProjectBuilder#buildProjectWithDependencies(File, ProjectBuilderConfiguration)} instead.
     */
    MavenProjectBuildingResult buildProjectWithDependencies( File project,
                                                             ArtifactRepository localRepository,
                                                             ProfileManager globalProfileManager )
        throws ProjectBuildingException;


    MavenProject buildFromRepository( Artifact artifact,
                                      List remoteArtifactRepositories,
                                      ArtifactRepository localRepository )
        throws ProjectBuildingException;

    /** @deprecated We do not want to allow projects to not have POMs. Maven 1.x repositories be damned. */
    MavenProject buildFromRepository( Artifact artifact,
                                      List remoteArtifactRepositories,
                                      ArtifactRepository localRepository,
                                      boolean allowStub )
        throws ProjectBuildingException;

    /**
     * @deprecated Use {@link MavenProjectBuilder#buildStandaloneSuperProject(ProjectBuilderConfiguration)} instead.
     */
    MavenProject buildStandaloneSuperProject()
        throws ProjectBuildingException;

    /**
     * @deprecated Use {@link MavenProjectBuilder#buildStandaloneSuperProject(ProjectBuilderConfiguration)} instead.
     */
    MavenProject buildStandaloneSuperProject( ProfileManager profileManager )
        throws ProjectBuildingException;

    MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException;
}
