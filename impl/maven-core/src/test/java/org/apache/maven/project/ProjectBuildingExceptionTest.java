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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link ProjectBuildingException} message generation.
 */
@SuppressWarnings("deprecation")
class ProjectBuildingExceptionTest {

    @Test
    void testDetailedExceptionMessageWithMultipleProblems() {
        List<ProjectBuildingResult> results = new ArrayList<>();

        List<ModelProblem> problems1 = new ArrayList<>();
        Collections.addAll(
                problems1,
                new DefaultModelProblem(
                        "Missing required dependency",
                        ModelProblem.Severity.ERROR,
                        null,
                        "pom.xml",
                        25,
                        10,
                        null,
                        null),
                new DefaultModelProblem(
                        "Invalid version format", ModelProblem.Severity.ERROR, null, "pom.xml", 30, 5, null, null));
        DefaultProjectBuildingResult result1 =
                new DefaultProjectBuildingResult("com.example:project1:1.0", new File("project1/pom.xml"), problems1);
        results.add(result1);

        List<ModelProblem> problems2 = new ArrayList<>();
        Collections.addAll(
                problems2,
                new DefaultModelProblem(
                        "Deprecated plugin usage", ModelProblem.Severity.WARNING, null, "pom.xml", 15, 3, null, null));
        DefaultProjectBuildingResult result2 =
                new DefaultProjectBuildingResult("com.example:project2:1.0", new File("project2/pom.xml"), problems2);
        results.add(result2);

        ProjectBuildingException exception = new ProjectBuildingException(results);
        String message = exception.getMessage();

        assertTrue(
                message.contains("3 problems were encountered while processing the POMs (2 errors)"),
                "Message should contain problem count and error count");

        assertTrue(message.contains("[com.example:project1:1.0]"), "Message should contain project1 identifier");

        assertTrue(message.contains("[com.example:project2:1.0]"), "Message should contain project2 identifier");

        assertTrue(
                message.contains("[ERROR] Missing required dependency @ pom.xml, line 25, column 10"),
                "Message should contain error details with location");

        assertTrue(
                message.contains("[ERROR] Invalid version format @ pom.xml, line 30, column 5"),
                "Message should contain second error details");

        assertTrue(
                !message.contains("[WARNING]") || message.contains("[WARNING] Deprecated plugin usage"),
                "Warnings should be filtered when errors are present or shown if explicitly included");
    }

    @Test
    void testExceptionMessageWithOnlyWarnings() {
        List<ProjectBuildingResult> results = new ArrayList<>();

        List<ModelProblem> problems = new ArrayList<>();
        Collections.addAll(
                problems,
                new DefaultModelProblem(
                        "Deprecated feature used", ModelProblem.Severity.WARNING, null, "pom.xml", 10, 1, null, null));
        DefaultProjectBuildingResult result =
                new DefaultProjectBuildingResult("com.example:project:1.0", new File("project/pom.xml"), problems);
        results.add(result);

        ProjectBuildingException exception = new ProjectBuildingException(results);
        String message = exception.getMessage();

        assertTrue(
                message.contains("1 problem was encountered while processing the POMs"),
                "Message should use singular form for single problem");

        assertTrue(
                message.contains("[WARNING] Deprecated feature used"),
                "Message should contain warning when no errors are present");

        assertTrue(
                !message.contains("(") || !message.contains("error"),
                "Message should not contain error count when there are no errors");
    }

    @Test
    void testExceptionMessageWithEmptyResults() {
        List<ProjectBuildingResult> results = Collections.emptyList();

        ProjectBuildingException exception = new ProjectBuildingException(results);
        String message = exception.getMessage();

        assertEquals(
                "Some problems were encountered while processing the POMs",
                message,
                "Empty results should fall back to generic message");
    }

    @Test
    void testExceptionMessageWithNullResults() {
        ProjectBuildingException exception = new ProjectBuildingException((List<ProjectBuildingResult>) null);
        String message = exception.getMessage();

        assertEquals(
                "Some problems were encountered while processing the POMs",
                message,
                "Null results should fall back to generic message");
    }

    @Test
    void testExceptionMessageWithUnknownProject() {
        List<ProjectBuildingResult> results = new ArrayList<>();

        List<ModelProblem> problems = new ArrayList<>();
        Collections.addAll(
                problems,
                new DefaultModelProblem("Some error", ModelProblem.Severity.ERROR, null, "unknown", 1, 1, null, null));
        DefaultProjectBuildingResult result = new DefaultProjectBuildingResult(null, null, problems);
        results.add(result);

        ProjectBuildingException exception = new ProjectBuildingException(results);
        String message = exception.getMessage();

        assertTrue(message.contains("[unknown project]"), "Message should handle unknown project gracefully");
    }
}
