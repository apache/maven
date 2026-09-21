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

import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.ModuleReport;
import org.apache.maven.api.build.report.MojoReport;

/**
 * Internal immutable implementation of {@link ModuleReport}.
 */
record DefaultModuleReport(
        String groupId,
        String artifactId,
        String version,
        BuildStatus status,
        Instant startTime,
        Duration duration,
        List<MojoReport> mojos,
        List<LogEvent> output)
        implements ModuleReport {

    @Override
    public List<MojoReport> mojos() {
        return List.copyOf(mojos);
    }

    @Override
    public List<LogEvent> output() {
        return List.copyOf(output);
    }
}
