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

import static java.util.stream.Collectors.joining;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Hamcrest matcher to help create fluent assertions about {@link ProjectBuildingResult} instances.
 */
class ProjectBuildingResultWithLocationMatcher extends BaseMatcher<ProjectBuildingResult> {
    private final int columnNumber;
    private final int lineNumber;

    ProjectBuildingResultWithLocationMatcher(int columnNumber, int lineNumber) {
        this.columnNumber = columnNumber;
        this.lineNumber = lineNumber;
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof ProjectBuildingResult)) {
            return false;
        }

        final ProjectBuildingResult r = (ProjectBuildingResult) o;

        return r.getProblems().stream()
                .anyMatch(p -> p.getLineNumber() == lineNumber && p.getColumnNumber() == columnNumber);
    }

    @Override
    public void describeTo(Description description) {
        description
                .appendText("a ProjectBuildingResult with location ")
                .appendText(formatLocation(columnNumber, lineNumber));
    }

    private String formatLocation(int columnNumber, int lineNumber) {
        return String.format("line %d, column %d", lineNumber, columnNumber);
    }

    @Override
    public void describeMismatch(final Object o, final Description description) {
        if (!(o instanceof ProjectBuildingResult)) {
            super.describeMismatch(o, description);
        } else {
            final ProjectBuildingResult r = (ProjectBuildingResult) o;
            description.appendText("was a ProjectBuildingResult with locations ");
            String messages = r.getProblems().stream()
                    .map(p -> formatLocation(p.getColumnNumber(), p.getLineNumber()))
                    .collect(joining(", "));
            description.appendText(messages);
        }
    }

    static Matcher<ProjectBuildingResult> projectBuildingResultWithLocation(int columnNumber, int lineNumber) {
        return new ProjectBuildingResultWithLocationMatcher(columnNumber, lineNumber);
    }
}
