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

import org.apache.maven.model.building.ModelProblem;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit custom assertion to help create fluent assertions about {@link ProjectBuildingResult} instances.
 */
class ProjectBuildingResultWithProblemMessageAssert {
    private final ProjectBuildingResult actual;

    ProjectBuildingResultWithProblemMessageAssert(ProjectBuildingResult actual) {
        this.actual = actual;
    }

    public static ProjectBuildingResultWithProblemMessageAssert assertThat(ProjectBuildingResult actual) {
        return new ProjectBuildingResultWithProblemMessageAssert(actual);
    }

    public ProjectBuildingResultWithProblemMessageAssert hasProblemMessage(String problemMessage) {
        assertNotNull(actual);

        boolean hasMessage =
                actual.getProblems().stream().anyMatch(p -> p.getMessage().contains(problemMessage));

        if (!hasMessage) {
            String actualMessages = actual.getProblems().stream()
                    .map(ModelProblem::getMessage)
                    .map(m -> "\"" + m + "\"")
                    .collect(joining(", "));
            String message = String.format(
                    "Expected ProjectBuildingResult to have problem message containing <%s> but had messages <%s>",
                    problemMessage, actualMessages);
            assertTrue(false, message);
        }

        return this;
    }

    // Helper method for backward compatibility
    static ProjectBuildingResultWithProblemMessageAssert projectBuildingResultWithProblemMessage(String message) {
        return new ProjectBuildingResultWithProblemMessageAssert(null).hasProblemMessage(message);
    }
}
