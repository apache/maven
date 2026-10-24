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
package org.apache.maven.internal.build;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.ReactorFailureBehavior;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.build.report.MojoReport;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildReportJsonWriterTest {

    private static LogEvent logEvent(Instant ts, LogLevel level, String msg, String logger) {
        return new LogEvent() {
            @Override
            public Instant timestamp() {
                return ts;
            }

            @Override
            public LogLevel level() {
                return level;
            }

            @Override
            public String message() {
                return msg;
            }

            @Override
            public String loggerName() {
                return logger;
            }

            @Override
            public Throwable throwable() {
                return null;
            }

            @Override
            public String stackTrace() {
                return null;
            }
        };
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static LogEvent logEventWithMeta(
            Instant ts,
            LogLevel level,
            String msg,
            String logger,
            String srcClass,
            String srcMethod,
            long threadId,
            long seqNum) {
        return new LogEvent() {
            @Override
            public Instant timestamp() {
                return ts;
            }

            @Override
            public LogLevel level() {
                return level;
            }

            @Override
            public String message() {
                return msg;
            }

            @Override
            public String loggerName() {
                return logger;
            }

            @Override
            public Throwable throwable() {
                return null;
            }

            @Override
            public String stackTrace() {
                return null;
            }

            @Override
            public String sourceClassName() {
                return srcClass;
            }

            @Override
            public String sourceMethodName() {
                return srcMethod;
            }

            @Override
            public long threadId() {
                return threadId;
            }

            @Override
            public long sequenceNumber() {
                return seqNum;
            }
        };
    }

    private static final BuildInvocation EMPTY_ENV = new DefaultBuildInvocation(
            List.of(),
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            List.of(),
            null,
            null,
            ReactorFailureBehavior.FAIL_FAST,
            false,
            false,
            false,
            true,
            false,
            1);
    private static final Instant BASE_TIME = Instant.parse("2025-01-15T10:30:00Z");

    @Test
    void testSuccessfulBuildReport() {
        List<LogEvent> mojoOutput = List.of(
                logEvent(BASE_TIME.plusSeconds(6), LogLevel.INFO, "Compiling 42 source files", "o.a.m.compiler"),
                logEvent(BASE_TIME.plusSeconds(7), LogLevel.INFO, "BUILD SUCCESS", "o.a.m.compiler"));

        MojoReport mojo = new DefaultMojoReport(
                "org.apache.maven.plugins",
                "maven-compiler-plugin",
                "3.15.0",
                "compile",
                "default-compile",
                "compile",
                BuildStatus.SUCCESS,
                BASE_TIME.plusSeconds(5),
                Duration.ofMillis(2100),
                "0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                mojoOutput);

        List<LogEvent> moduleOutput = List.of(logEvent(
                BASE_TIME.plusSeconds(2), LogLevel.INFO, "Resolving dependencies for maven-core", "o.a.m.resolver"));

        ModuleReport module = new DefaultModuleReport(
                "org.apache.maven",
                "maven-core",
                "4.1.0-SNAPSHOT",
                BuildStatus.SUCCESS,
                BASE_TIME.plusSeconds(1),
                Duration.ofMillis(12345),
                "fedcba9876543210fedcba9876543210",
                "fedcba9876543210",
                List.of(mojo),
                moduleOutput);

        List<LogEvent> buildOutput = List.of(
                logEvent(BASE_TIME, LogLevel.INFO, "Reactor Build Order:", "o.a.m.reactor"),
                logEvent(BASE_TIME, LogLevel.INFO, "Maven Core", "o.a.m.reactor"));

        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ofMillis(30000),
                BASE_TIME,
                "4.1.0-SNAPSHOT",
                "21.0.1",
                List.of("clean", "install"),
                "org.apache.maven:maven:4.1.0-SNAPSHOT",
                true,
                4,
                List.of(module),
                List.of(),
                List.of(),
                buildOutput);

        String json = BuildReportJsonWriter.toJson(report);

        assertTrue(json.contains("\"formatVersion\": 1"));
        assertTrue(json.contains("\"reportVersion\": \"1.1\""));
        assertTrue(json.contains("\"status\": \"SUCCESS\""));
        assertTrue(json.contains("\"mavenVersion\": \"4.1.0-SNAPSHOT\""));
        assertTrue(json.contains("\"javaVersion\": \"21.0.1\""));
        assertTrue(json.contains("\"goals\": [\"clean\", \"install\"]"));
        assertTrue(json.contains("\"multiModule\": true"));
        assertTrue(json.contains("\"threads\": 4"));
        assertTrue(json.contains("\"groupId\": \"org.apache.maven\""));
        assertTrue(json.contains("\"artifactId\": \"maven-core\""));
        assertTrue(json.contains("\"goal\": \"compile\""));
        assertTrue(json.contains("\"executionId\": \"default-compile\""));
        assertTrue(json.contains("\"traceId\": \"0123456789abcdef0123456789abcdef\""));
        assertTrue(json.contains("\"spanId\": \"0123456789abcdef\""));
        assertTrue(json.contains("\"traceId\": \"fedcba9876543210fedcba9876543210\""));
        assertTrue(json.contains("\"spanId\": \"fedcba9876543210\""));
        assertTrue(json.contains("\"failures\": []"));
        // Module and mojo start times
        assertTrue(json.contains("\"startTime\": \"2025-01-15T10:30:01Z\""), "module startTime");
        assertTrue(json.contains("\"startTime\": \"2025-01-15T10:30:05Z\""), "mojo startTime");
        // Structured log events at all three levels
        assertTrue(json.contains("\"message\": \"Compiling 42 source files\""), "mojo-level log event");
        assertTrue(json.contains("\"message\": \"Resolving dependencies for maven-core\""), "module-level log event");
        assertTrue(json.contains("\"message\": \"Reactor Build Order:\""), "build-level log event");
        // Log event structure
        assertTrue(json.contains("\"level\": \"INFO\""), "log level");
        assertTrue(json.contains("\"loggerName\": \"o.a.m.compiler\""), "logger name");
    }

    @Test
    void testFailedBuildReport() {
        FailureReport failure = new DefaultFailureReport(
                "org.apache.maven:maven-core:4.1.0-SNAPSHOT",
                "maven-compiler-plugin:3.15.0:compile",
                BASE_TIME.plusSeconds(3),
                "CompilationFailureException",
                "Compilation failure: 3 errors",
                "org.apache.maven.plugin.compiler.CompilationFailureException: ...\n\tat ...\n");

        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.FAILURE,
                Duration.ofMillis(5000),
                BASE_TIME,
                "4.1.0-SNAPSHOT",
                "21.0.1",
                List.of("compile"),
                "org.apache.maven:maven-core:4.1.0-SNAPSHOT",
                false,
                1,
                List.of(),
                List.of(failure),
                List.of(),
                List.of());

        String json = BuildReportJsonWriter.toJson(report);

        assertTrue(json.contains("\"status\": \"FAILURE\""));
        assertTrue(json.contains("\"module\": \"org.apache.maven:maven-core:4.1.0-SNAPSHOT\""));
        assertTrue(json.contains("\"mojo\": \"maven-compiler-plugin:3.15.0:compile\""));
        assertTrue(json.contains("\"message\": \"Compilation failure: 3 errors\""));
        assertTrue(json.contains("\"stackTrace\""));
        // Enriched fields
        assertTrue(json.contains("\"timestamp\": \"2025-01-15T10:30:03Z\""), "failure timestamp");
        assertTrue(json.contains("\"exceptionType\": \"CompilationFailureException\""), "failure exceptionType");
    }

    @Test
    void testJsonStringEscaping() {
        FailureReport failure = new DefaultFailureReport(
                "com.example:test:1.0",
                null,
                BASE_TIME,
                null,
                "Error: \"unexpected\" value\nwith newline\tand tab",
                null);

        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.FAILURE,
                Duration.ofMillis(100),
                BASE_TIME,
                "4.1.0",
                "21",
                List.of(),
                "com.example:test:1.0",
                false,
                1,
                List.of(),
                List.of(failure),
                List.of(),
                List.of());

        String json = BuildReportJsonWriter.toJson(report);

        // Check proper JSON escaping
        assertTrue(json.contains("\\\"unexpected\\\""));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
    }

    @Test
    void testEmptyModulesAndFailures() {
        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ofMillis(100),
                BASE_TIME,
                "4.1.0",
                "21",
                List.of(),
                "com.example:test:1.0",
                false,
                1,
                List.of(),
                List.of(),
                List.of(),
                List.of());

        String json = BuildReportJsonWriter.toJson(report);

        assertTrue(json.contains("\"modules\": []"));
        assertTrue(json.contains("\"failures\": []"));
    }

    @Test
    void testFormatVersion() {
        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ZERO,
                BASE_TIME,
                "4.1.0",
                "21",
                List.of(),
                "test:test:1.0",
                false,
                1,
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertEquals(1, report.formatVersion());
    }

    @Test
    void testMultipleModules() {
        ModuleReport mod1 = new DefaultModuleReport(
                "com.example",
                "api",
                "1.0",
                BuildStatus.SUCCESS,
                BASE_TIME.plusSeconds(1),
                Duration.ofSeconds(5),
                null,
                null,
                List.of(),
                List.of());
        ModuleReport mod2 = new DefaultModuleReport(
                "com.example",
                "impl",
                "1.0",
                BuildStatus.SUCCESS,
                BASE_TIME.plusSeconds(6),
                Duration.ofSeconds(10),
                null,
                null,
                List.of(),
                List.of());
        ModuleReport mod3 = new DefaultModuleReport(
                "com.example",
                "web",
                "1.0",
                BuildStatus.SKIPPED,
                BASE_TIME.plusSeconds(16),
                Duration.ZERO,
                null,
                null,
                List.of(),
                List.of());

        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ofSeconds(15),
                BASE_TIME,
                "4.1.0",
                "21",
                List.of("install"),
                "com.example:parent:1.0",
                true,
                1,
                List.of(mod1, mod2, mod3),
                List.of(),
                List.of(),
                List.of());

        String json = BuildReportJsonWriter.toJson(report);

        // All modules present
        assertTrue(json.contains("\"artifactId\": \"api\""));
        assertTrue(json.contains("\"artifactId\": \"impl\""));
        assertTrue(json.contains("\"artifactId\": \"web\""));
        assertTrue(json.contains("\"status\": \"SKIPPED\""));
    }

    @Test
    void testNullMojoFields() {
        // mojo with null executionId and phase (direct invocation)
        MojoReport mojo = new DefaultMojoReport(
                "org.apache.maven.plugins",
                "maven-help-plugin",
                "3.4.1",
                "effective-pom",
                null,
                null,
                BuildStatus.SUCCESS,
                BASE_TIME.plusSeconds(1),
                Duration.ofMillis(500),
                null,
                null,
                List.of());

        String json = BuildReportJsonWriter.toJson(new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ofSeconds(1),
                BASE_TIME,
                "4.1.0",
                "21",
                List.of("help:effective-pom"),
                "test:test:1.0",
                false,
                1,
                List.of(new DefaultModuleReport(
                        "test",
                        "test",
                        "1.0",
                        BuildStatus.SUCCESS,
                        BASE_TIME,
                        Duration.ofSeconds(1),
                        null,
                        null,
                        List.of(mojo),
                        List.of())),
                List.of(),
                List.of(),
                List.of()));

        assertTrue(json.contains("\"executionId\": null"));
        assertTrue(json.contains("\"phase\": null"));
        assertFalse(json.contains("\"executionId\": \"null\""));
    }

    @Test
    void testLogEventWithJulMetadata() {
        // LogEvent with source class and method metadata
        LogEvent julEvent = logEventWithMeta(
                BASE_TIME.plusSeconds(1),
                LogLevel.WARN,
                "Unsupported class file major version 65",
                "org.apache.maven.plugins.compiler",
                "com.sun.tools.javac.processing.JavacProcessingEnvironment",
                "doProcessing",
                42L,
                1001L);

        // LogEvent without JUL metadata (from SLF4J)
        LogEvent slf4jEvent = logEvent(BASE_TIME.plusSeconds(2), LogLevel.INFO, "Compiling 10 files", "o.a.m.compiler");

        MojoReport mojo = new DefaultMojoReport(
                "org.apache.maven.plugins",
                "maven-compiler-plugin",
                "3.15.0",
                "compile",
                "default-compile",
                "compile",
                BuildStatus.SUCCESS,
                BASE_TIME,
                Duration.ofMillis(1000),
                null,
                null,
                List.of(julEvent, slf4jEvent));

        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ofSeconds(1),
                BASE_TIME,
                "4.1.0",
                "21",
                List.of("compile"),
                "test:test:1.0",
                false,
                1,
                List.of(new DefaultModuleReport(
                        "test",
                        "test",
                        "1.0",
                        BuildStatus.SUCCESS,
                        BASE_TIME,
                        Duration.ofSeconds(1),
                        null,
                        null,
                        List.of(mojo),
                        List.of())),
                List.of(),
                List.of(),
                List.of());

        String json = BuildReportJsonWriter.toJson(report);

        // JUL event should have sourceClassName, sourceMethodName, threadId, and sequenceNumber
        assertTrue(
                json.contains("\"sourceClassName\": \"com.sun.tools.javac.processing.JavacProcessingEnvironment\""),
                "sourceClassName");
        assertTrue(json.contains("\"sourceMethodName\": \"doProcessing\""), "sourceMethodName");
        assertTrue(json.contains("\"threadId\": 42"), "threadId");
        assertTrue(json.contains("\"sequenceNumber\": 1001"), "sequenceNumber");
        // SLF4J event should NOT have JUL fields
        // (the second log event in the output array has no sourceClassName)
        assertFalse(
                json.contains("\"sourceClassName\": \"o.a.m.compiler\""),
                "SLF4J event should not have sourceClassName");
    }

    @Test
    void testEmptyProblems() {
        BuildReport report = new DefaultBuildReport(
                EMPTY_ENV,
                BuildStatus.SUCCESS,
                Duration.ofMillis(100),
                BASE_TIME,
                "4.1.0",
                "21",
                List.of(),
                "com.example:test:1.0",
                false,
                1,
                List.of(),
                List.of(),
                List.of(),
                List.of());

        String json = BuildReportJsonWriter.toJson(report);
        assertTrue(json.contains("\"problems\": []"), "empty problems array");
    }
}
