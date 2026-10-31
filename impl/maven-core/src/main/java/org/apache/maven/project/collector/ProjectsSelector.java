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
package org.apache.maven.project.collector;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

/**
 * Facade to select projects for a given set of pom.xml files.
 */
public interface ProjectsSelector {
    /**
     * Select Maven projects from a list of POM files.
     * @param files List of POM files.
     * @param request The {@link MavenExecutionRequest}
     * @return A list of projects that have been found in the specified POM files.
     * @throws ProjectBuildingException In case the POMs are not used.
     */
    List<MavenProject> selectProjects(List<File> files, MavenExecutionRequest request) throws ProjectBuildingException;

    /**
     * Select Maven projects from a list of POM files and report model problems encountered while building them.
     *
     * <p>The default implementation delegates to {@link #selectProjects(List, MavenExecutionRequest)}
     * for compatibility with existing implementations and does not invoke {@code problemConsumer}.
     * Implementations must override this method to report model problems to the consumer.
     *
     * @param files List of POM files.
     * @param request The {@link MavenExecutionRequest}
     * @param problemConsumer Consumer for model problems encountered while building the selected projects.
     * @return A list of projects that have been found in the specified POM files.
     * @throws ProjectBuildingException In case the POMs are not used.
     */
    default List<MavenProject> selectProjects(
            List<File> files, MavenExecutionRequest request, Consumer<ModelProblem> problemConsumer)
            throws ProjectBuildingException {
        return selectProjects(files, request);
    }
}
