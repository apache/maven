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
package org.apache.maven.cling.invoker.mvnlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.apache.maven.jline.MessageUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildReportRendererTest {

    private final JLineMessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();

    @BeforeAll
    static void setUp() {
        MessageUtils.setColorEnabled(false);
    }

    @AfterAll
    static void tearDown() {
        MessageUtils.setColorEnabled(true);
    }

    private Map<String, Object> createSuccessReport() {
        String json = """
                {
                  "formatVersion": "1.0",
                  "status": "SUCCESS",
                  "duration": "PT6.7S",
                  "startTime": "2026-07-29T10:00:00Z",
                  "mavenVersion": "4.1.0-SNAPSHOT",
                  "javaVersion": "21.0.1",
                  "goals": ["clean", "install"],
                  "project": "org.example:root",
                  "multiModule": true,
                  "threads": 1,
                  "modules": [
                    {
                      "groupId": "org.example",
                      "artifactId": "api",
                      "version": "1.0",
                      "status": "SUCCESS",
                      "startTime": "2026-07-29T10:00:01Z",
                      "duration": "PT2.1S",
                      "mojos": [
                        {
                          "groupId": "org.apache.maven.plugins",
                          "artifactId": "maven-compiler-plugin",
                          "version": "3.15.0",
                          "goal": "compile",
                          "executionId": "default-compile",
                          "phase": "compile",
                          "status": "SUCCESS",
                          "startTime": "2026-07-29T10:00:01Z",
                          "duration": "PT1.5S",
                          "output": []
                        }
                      ],
                      "output": []
                    },
                    {
                      "groupId": "org.example",
                      "artifactId": "core",
                      "version": "1.0",
                      "status": "SUCCESS",
                      "startTime": "2026-07-29T10:00:03Z",
                      "duration": "PT3.4S",
                      "mojos": [],
                      "output": []
                    }
                  ],
                  "problems": [
                    {
                      "key": "deprecated-source-target",
                      "severity": "WARNING",
                      "message": "source/target value 8 is obsolete and will be removed in a future release",
                      "source": "maven-compiler-plugin:3.15.0:compile",
                      "suggestion": "Update maven.compiler.source to 11 or higher",
                      "documentationUrl": "https://maven.apache.org/plugins/maven-compiler-plugin/"
                    }
                  ],
                  "failures": [],
                  "output": []
                }""";
        return SimpleJsonReader.parse(json);
    }

    private Map<String, Object> createFailureReport() {
        String json = """
                {
                  "formatVersion": "1.0",
                  "status": "FAILURE",
                  "duration": "PT5.0S",
                  "startTime": "2026-07-29T10:00:00Z",
                  "mavenVersion": "4.1.0-SNAPSHOT",
                  "javaVersion": "21.0.1",
                  "goals": ["compile"],
                  "project": "org.example:root",
                  "multiModule": false,
                  "threads": 1,
                  "modules": [
                    {
                      "groupId": "org.example",
                      "artifactId": "core",
                      "version": "1.0",
                      "status": "FAILURE",
                      "startTime": "2026-07-29T10:00:01Z",
                      "duration": "PT5.0S",
                      "mojos": [],
                      "output": []
                    }
                  ],
                  "problems": [],
                  "failures": [
                    {
                      "module": "org.example:core",
                      "mojo": "compiler:compile",
                      "timestamp": "2026-07-29T10:00:05Z",
                      "message": "Compilation failure",
                      "stackTrace": "org.apache.maven.lifecycle.LifecycleExecutionException\\nat Lifecycle.java:42"
                    }
                  ],
                  "output": []
                }""";
        return SimpleJsonReader.parse(json);
    }

    @Test
    void testRenderSummarySuccess() {
        List<String> lines = new ArrayList<>();
        BuildReportRenderer renderer = new BuildReportRenderer(messageBuilderFactory, lines::add);

        renderer.renderSummary(createSuccessReport());

        String output = String.join("\n", lines);
        assertTrue(output.contains("Build Report"), "Should contain header");
        assertTrue(output.contains("Maven 4.1.0-SNAPSHOT"), "Should contain Maven version");
        assertTrue(output.contains("BUILD SUCCESS"), "Should contain BUILD SUCCESS");
        assertTrue(output.contains("api"), "Should contain first module");
        assertTrue(output.contains("core"), "Should contain second module");
        assertTrue(output.contains("2 modules"), "Should contain module count");
        assertTrue(output.contains("2 passed"), "Should contain passed count");
        assertTrue(output.contains("1 warning"), "Should contain warning count");
        assertTrue(output.contains("source/target value 8"), "Should show actual warning message in default view");
        assertTrue(output.contains("maven-compiler-plugin"), "Should show warning source in default view");
        assertTrue(output.contains("Update maven.compiler.source"), "Should show suggestion in default view");
        assertTrue(output.contains("Total time:  6.700 s"), "Should contain formatted total time");
    }

    @Test
    void testRenderSummaryFailure() {
        List<String> lines = new ArrayList<>();
        BuildReportRenderer renderer = new BuildReportRenderer(messageBuilderFactory, lines::add);

        renderer.renderSummary(createFailureReport());

        String output = String.join("\n", lines);
        assertTrue(output.contains("BUILD FAILURE"), "Should contain BUILD FAILURE");
        assertTrue(output.contains("1 failure"), "Should contain failure count");
    }

    @Test
    void testRenderDiagnostics() {
        List<String> lines = new ArrayList<>();
        BuildReportRenderer renderer = new BuildReportRenderer(messageBuilderFactory, lines::add);

        renderer.renderDiagnostics(createSuccessReport());

        String output = String.join("\n", lines);
        assertTrue(output.contains("Problems (1)"), "Should contain problems header");
        assertTrue(output.contains("source/target value 8"), "Should contain warning message");
        assertTrue(output.contains("deprecated-source-target"), "Should contain problem key");
        assertTrue(output.contains("maven-compiler-plugin"), "Should contain source");
        assertTrue(output.contains("Update maven.compiler.source"), "Should contain suggestion");
        assertTrue(
                output.contains("https://maven.apache.org/plugins/maven-compiler-plugin/"),
                "Should contain documentation URL");
    }

    @Test
    void testRenderDiagnosticsWhenEmpty() {
        List<String> lines = new ArrayList<>();
        BuildReportRenderer renderer = new BuildReportRenderer(messageBuilderFactory, lines::add);

        renderer.renderDiagnostics(createFailureReport());

        String output = String.join("\n", lines);
        assertTrue(output.contains("No problems recorded"), "Should show empty message");
    }

    @Test
    void testRenderFailures() {
        List<String> lines = new ArrayList<>();
        BuildReportRenderer renderer = new BuildReportRenderer(messageBuilderFactory, lines::add);

        renderer.renderFailures(createFailureReport());

        String output = String.join("\n", lines);
        assertTrue(output.contains("Failures (1)"), "Should contain failures header");
        assertTrue(output.contains("org.example:core"), "Should contain module name");
        assertTrue(output.contains("compiler:compile"), "Should contain mojo");
        assertTrue(output.contains("Compilation failure"), "Should contain error message");
    }

    @Test
    void testRenderFull() {
        List<String> lines = new ArrayList<>();
        BuildReportRenderer renderer = new BuildReportRenderer(messageBuilderFactory, lines::add);

        renderer.renderFull(createSuccessReport());

        String output = String.join("\n", lines);
        assertTrue(output.contains("Module: api"), "Should contain module name");
        assertTrue(output.contains("compiler"), "Should contain mojo plugin");
        assertTrue(output.contains("compile"), "Should contain mojo goal");
        assertTrue(output.contains("default-compile"), "Should contain execution id");
    }

    @Test
    void testFormatDuration() {
        assertEquals("6.700 s", BuildReportRenderer.formatDuration("PT6.7S"));
        assertEquals("0.100 s", BuildReportRenderer.formatDuration("PT0.1S"));
        assertEquals("1:30 min", BuildReportRenderer.formatDuration("PT1M30S"));
        assertEquals("PT-invalid", BuildReportRenderer.formatDuration("PT-invalid")); // fallback
    }
}
