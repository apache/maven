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

import org.assertj.core.api.AbstractAssert;

import static java.util.stream.Collectors.joining;

/**
 * AssertJ custom assertion to help create fluent assertions about {@link ProjectBuildingResult} instances.
 */
class ProjectBuildingResultWithLocationAssert
        extends AbstractAssert<ProjectBuildingResultWithLocationAssert, ProjectBuildingResult> {

    ProjectBuildingResultWithLocationAssert(ProjectBuildingResult actual) {
        super(actual, ProjectBuildingResultWithLocationAssert.class);
    }

    public static ProjectBuildingResultWithLocationAssert assertThat(ProjectBuildingResult actual) {
        return new ProjectBuildingResultWithLocationAssert(actual);
    }

    public ProjectBuildingResultWithLocationAssert hasLocation(int columnNumber, int lineNumber) {
        isNotNull();

        boolean hasLocation = actual.getProblems().stream()
                .anyMatch(p -> p.getLineNumber() == lineNumber && p.getColumnNumber() == columnNumber);

        if (!hasLocation) {
            String actualLocations = actual.getProblems().stream()
                    .map(p -> formatLocation(p.getColumnNumber(), p.getLineNumber()))
                    .collect(joining(", "));
            failWithMessage(
                    "Expected ProjectBuildingResult to have location <%s> but had locations <%s>",
                    formatLocation(columnNumber, lineNumber), actualLocations);
        }

        return this;
    }

    private String formatLocation(int columnNumber, int lineNumber) {
        return String.format("line %d, column %d", lineNumber, columnNumber);
    }

    // Helper method for backward compatibility
    static ProjectBuildingResultWithLocationAssert projectBuildingResultWithLocation(int columnNumber, int lineNumber) {
        return new ProjectBuildingResultWithLocationAssert(null).hasLocation(columnNumber, lineNumber);
    }
}
