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
package com.example;

import java.util.Objects;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.Result;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class BuildExtensionUsingGraphPackage extends AbstractMavenLifecycleParticipant {

    @Requirement(hint = GraphBuilder.HINT)
    private GraphBuilder graphBuilder;

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        Objects.requireNonNull(graphBuilder, "graphBuilder should be available in build extension");

        Result<? extends ProjectDependencyGraph> graphResult = graphBuilder.build(session);
        Objects.requireNonNull(graphResult, "graphResult should have been built");

        for (ModelProblem problem : graphResult.getProblems()) {
            if (problem.getSeverity() == ModelProblem.Severity.WARNING) {
                throw new IllegalStateException("unexpected WARNING found: " + problem);
            } else {
                throw new IllegalStateException("unexpected Problem found: " + problem);
            }
        }
    }
}
