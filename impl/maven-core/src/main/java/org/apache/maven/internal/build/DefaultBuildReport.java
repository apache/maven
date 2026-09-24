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

import org.apache.maven.api.BuildEnvironment;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.services.BuilderProblem;

/**
 * Internal immutable implementation of {@link BuildReport}.
 */
record DefaultBuildReport(
        BuildEnvironment environment,
        BuildStatus status,
        Duration duration,
        Instant startTime,
        String mavenVersion,
        String javaVersion,
        List<String> goals,
        String project,
        boolean multiModule,
        int threads,
        List<ModuleReport> modules,
        List<FailureReport> failures,
        List<BuilderProblem> problems,
        List<LogEvent> output)
        implements BuildReport {

    DefaultBuildReport {
        goals = List.copyOf(goals);
        modules = List.copyOf(modules);
        failures = List.copyOf(failures);
        problems = List.copyOf(problems);
        output = List.copyOf(output);
    }

    private static final int FORMAT_VERSION = 1;

    @Override
    public int formatVersion() {
        return FORMAT_VERSION;
    }
}
