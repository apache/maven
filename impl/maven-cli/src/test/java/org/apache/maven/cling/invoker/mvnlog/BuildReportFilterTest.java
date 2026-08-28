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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildReportFilterTest {

    @Test
    void noFiltersPassesThrough() {
        BuildReportFilter filter = new BuildReportFilter(null, null, null, null);
        assertFalse(filter.hasFilters());

        Map<String, Object> report = sampleReport();
        Map<String, Object> result = filter.apply(report);
        assertEquals(report, result);
    }

    @Test
    void moduleFilterKeepsMatchingModules() {
        BuildReportFilter filter = new BuildReportFilter("api", null, null, null);
        assertTrue(filter.hasFilters());

        Map<String, Object> result = filter.apply(sampleReport());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) result.get("modules");
        assertEquals(1, modules.size());
        assertEquals("maven-api-core", modules.get(0).get("artifactId"));
    }

    @Test
    void moduleFilterIsCaseInsensitive() {
        BuildReportFilter filter = new BuildReportFilter("API", null, null, null);
        Map<String, Object> result = filter.apply(sampleReport());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) result.get("modules");
        assertEquals(1, modules.size());
    }

    @Test
    void mojoFilterKeepsMatchingMojos() {
        BuildReportFilter filter = new BuildReportFilter(null, "resources", null, null);
        Map<String, Object> result = filter.apply(sampleReport());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) result.get("modules");
        // Both modules should remain
        assertEquals(2, modules.size());

        // Module 1 (api) has no resources mojo
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mojos0 =
                (List<Map<String, Object>>) modules.get(0).get("mojos");
        assertEquals(0, mojos0.size());

        // Module 2 (cli) has a resources mojo
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mojos1 =
                (List<Map<String, Object>>) modules.get(1).get("mojos");
        assertEquals(1, mojos1.size());
        assertEquals("resources", mojos1.get(0).get("goal"));
    }

    @Test
    void levelFilterKeepsEventsAtOrAbove() {
        BuildReportFilter filter = new BuildReportFilter(null, null, "WARN", null);
        assertTrue(filter.hasLogFilters());

        Map<String, Object> report = sampleReport();
        List<Map<String, Object>> events = filter.collectMatchingLogEvents(report);
        // Should only include WARN and ERROR events
        for (Map<String, Object> event : events) {
            String level = (String) event.get("level");
            assertTrue("WARN".equals(level) || "ERROR".equals(level), "Unexpected level: " + level);
        }
        assertFalse(events.isEmpty(), "Should find at least one WARN/ERROR event");
    }

    @Test
    void grepFilterMatchesMessages() {
        BuildReportFilter filter = new BuildReportFilter(null, null, null, "deprecated");
        assertTrue(filter.hasLogFilters());

        Map<String, Object> report = sampleReport();
        List<Map<String, Object>> events = filter.collectMatchingLogEvents(report);
        assertEquals(1, events.size());
        assertTrue(((String) events.get(0).get("message")).contains("deprecated"));
    }

    @Test
    void grepFilterIsCaseInsensitive() {
        BuildReportFilter filter = new BuildReportFilter(null, null, null, "DEPRECATED");
        List<Map<String, Object>> events = filter.collectMatchingLogEvents(sampleReport());
        assertEquals(1, events.size());
    }

    @Test
    void combinedModuleAndLevelFilter() {
        BuildReportFilter filter = new BuildReportFilter("cli", null, "WARN", null);
        List<Map<String, Object>> events = filter.collectMatchingLogEvents(sampleReport());
        // Should only include WARN+ events from maven-cli module
        for (Map<String, Object> event : events) {
            String context = (String) event.get("context");
            assertTrue(context.contains("cli"), "Event context should contain 'cli': " + context);
        }
    }

    @Test
    void isAtOrAboveLevelComparison() {
        assertTrue(BuildReportFilter.isAtOrAbove("ERROR", "WARN"));
        assertTrue(BuildReportFilter.isAtOrAbove("WARN", "WARN"));
        assertFalse(BuildReportFilter.isAtOrAbove("INFO", "WARN"));
        assertFalse(BuildReportFilter.isAtOrAbove("DEBUG", "WARN"));
        assertTrue(BuildReportFilter.isAtOrAbove("INFO", "INFO"));
        assertTrue(BuildReportFilter.isAtOrAbove("ERROR", "TRACE"));
    }

    @Test
    void jsonFilteredOutputRemovesNonMatchingModules() {
        BuildReportFilter filter = new BuildReportFilter("api", null, null, null);
        Map<String, Object> result = filter.apply(sampleReport());
        // The result should still have all top-level keys
        assertTrue(result.containsKey("status"));
        assertTrue(result.containsKey("modules"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) result.get("modules");
        assertEquals(1, modules.size());
    }

    @Test
    void applyWithLevelFilterFiltersModuleOutput() {
        BuildReportFilter filter = new BuildReportFilter(null, null, "ERROR", null);
        Map<String, Object> result = filter.apply(sampleReport());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) result.get("modules");
        for (Map<String, Object> module : modules) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output = (List<Map<String, Object>>) module.get("output");
            for (Map<String, Object> event : output) {
                assertEquals("ERROR", event.get("level"));
            }
        }
    }

    // ---- Test data ----

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sampleReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", "SUCCESS");
        report.put("mavenVersion", "4.1.0-SNAPSHOT");
        report.put("duration", "PT10.5S");
        report.put(
                "output",
                List.of(
                        logEvent("INFO", "Building reactor..."),
                        logEvent("WARN", "Reactor build order could be optimized")));

        // Module 1: maven-api-core
        Map<String, Object> apiModule = new LinkedHashMap<>();
        apiModule.put("artifactId", "maven-api-core");
        apiModule.put("status", "SUCCESS");
        apiModule.put("duration", "PT3.2S");
        apiModule.put(
                "output",
                List.of(
                        logEvent("INFO", "Compiling 42 source files"),
                        logEvent("WARN", "Using deprecated API method")));
        apiModule.put(
                "mojos",
                List.of(
                        mojo("compile", List.of(logEvent("INFO", "Compiling sources"))),
                        mojo("test-compile", List.of(logEvent("INFO", "Compiling test sources")))));

        // Module 2: maven-cli
        Map<String, Object> cliModule = new LinkedHashMap<>();
        cliModule.put("artifactId", "maven-cli");
        cliModule.put("status", "SUCCESS");
        cliModule.put("duration", "PT4.1S");
        cliModule.put(
                "output",
                List.of(
                        logEvent("INFO", "Compiling 30 source files"),
                        logEvent("WARN", "Some warning in CLI"),
                        logEvent("ERROR", "Critical error in module")));
        cliModule.put(
                "mojos",
                List.of(
                        mojo("compile", List.of(logEvent("INFO", "Compiling sources"))),
                        mojo("resources", List.of(logEvent("DEBUG", "Copying resources")))));

        report.put("modules", List.of(apiModule, cliModule));
        report.put("problems", List.of());
        report.put("failures", List.of());
        return report;
    }

    private static Map<String, Object> logEvent(String level, String message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("level", level);
        event.put("message", message);
        event.put("loggerName", "org.apache.maven.TestLogger");
        event.put("timestamp", "2026-08-08T10:00:00Z");
        return event;
    }

    private static Map<String, Object> mojo(String goal, List<Map<String, Object>> output) {
        Map<String, Object> mojo = new LinkedHashMap<>();
        mojo.put("goal", goal);
        mojo.put("artifactId", "maven-compiler-plugin");
        mojo.put("status", "SUCCESS");
        mojo.put("duration", "PT1.5S");
        mojo.put("output", output);
        return mojo;
    }
}
