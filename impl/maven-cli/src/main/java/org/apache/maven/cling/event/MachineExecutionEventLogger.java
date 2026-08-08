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
package org.apache.maven.cling.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.cling.event.MachineBuildEventListener.JsonLine;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * Execution event logger for machine-readable output ({@code --console=machine}).
 * <p>
 * Emits one JSON line per lifecycle event to the shared
 * {@link MachineBuildEventListener#emitEvent(String)} writer. This logger
 * handles the {@link org.apache.maven.execution.ExecutionListener} events:
 * session start/end, project start/success/failure/skip, and mojo
 * start/success/failure/skip.
 * <p>
 * Together with {@link MachineBuildEventListener} (which handles log messages,
 * transfers, and execution failures), this provides a complete, typed event
 * stream suitable for piping to external tools, CI systems, and LLM agents.
 * <p>
 * Example output:
 * <pre>
 * {"event":"build.started","timestamp":"...","projectCount":12,"goals":"clean install"}
 * {"event":"module.started","timestamp":"...","module":"maven-core","groupId":"org.apache.maven","artifactId":"maven-core","version":"4.1.0-SNAPSHOT","index":1,"total":12}
 * {"event":"mojo.started","timestamp":"...","module":"maven-core","plugin":"maven-compiler-plugin","goal":"compile","phase":"compile"}
 * {"event":"mojo.succeeded","timestamp":"...","module":"maven-core","plugin":"maven-compiler-plugin","goal":"compile","duration":1.2}
 * {"event":"module.succeeded","timestamp":"...","module":"maven-core","duration":2.1}
 * {"event":"build.finished","timestamp":"...","status":"SUCCESS","duration":32.1,"total":12,"passed":12,"failed":0,"skipped":0}
 * </pre>
 *
 * Selected via {@code --console=machine}.
 *
 * @since 4.1.0
 * @see MachineBuildEventListener
 */
public class MachineExecutionEventLogger extends AbstractExecutionListener {

    private final MachineBuildEventListener machineBel;

    // Track mojo start times for duration calculation
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();

    // Reactor state
    private volatile int totalProjects;
    private volatile int currentVisitedProjectCount;
    private volatile Instant buildStartTime;

    public MachineExecutionEventLogger(MachineBuildEventListener machineBel) {
        this.machineBel = Objects.requireNonNull(machineBel, "machineBel cannot be null");
    }

    // ---- Session lifecycle ----

    @Override
    public void sessionStarted(ExecutionEvent event) {
        MavenSession session = event.getSession();
        List<MavenProject> projects = session.getProjects();
        List<MavenProject> allProjects = session.getAllProjects();

        totalProjects = allProjects.size();
        currentVisitedProjectCount = allProjects.size() - projects.size();
        buildStartTime = MonotonicClock.now();

        String goals = session.getRequest().getGoals().stream().collect(Collectors.joining(" "));

        JsonLine line = new JsonLine("build.started")
                .field("projectCount", totalProjects)
                .field("goals", goals);

        List<String> profiles = session.getRequest().getActiveProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            line.field("profiles", String.join(",", profiles));
        }

        machineBel.emitEvent(line.build());
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        MavenSession session = event.getSession();

        int passed = 0;
        int failed = 0;
        int skipped = 0;
        for (MavenProject project : session.getProjects()) {
            BuildSummary summary = session.getResult().getBuildSummary(project);
            if (summary instanceof BuildSuccess) {
                passed++;
            } else if (summary instanceof BuildFailure) {
                failed++;
            } else {
                skipped++;
            }
        }

        String status = session.getResult().hasExceptions() ? "FAILURE" : "SUCCESS";
        double duration = 0;
        if (buildStartTime != null) {
            duration = Duration.between(buildStartTime, MonotonicClock.now()).toMillis() / 1000.0;
        }

        machineBel.emitEvent(new JsonLine("build.finished")
                .field("status", status)
                .field("duration", duration)
                .field("total", totalProjects)
                .field("passed", passed)
                .field("failed", failed)
                .field("skipped", skipped)
                .build());
    }

    // ---- Module lifecycle ----

    @Override
    public void projectStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        int index;
        synchronized (this) {
            index = ++currentVisitedProjectCount;
        }

        machineBel.emitEvent(new JsonLine("module.started")
                .field("module", project.getName())
                .field("groupId", project.getGroupId())
                .field("artifactId", project.getArtifactId())
                .field("version", project.getVersion())
                .field("index", index)
                .field("total", totalProjects)
                .build());
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        logModuleFinished(event, "module.succeeded");
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        logModuleFinished(event, "module.failed");
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        MavenProject project = event.getProject();
        machineBel.emitEvent(new JsonLine("module.skipped")
                .field("module", project.getName())
                .build());
    }

    // ---- Mojo lifecycle ----

    @Override
    public void mojoStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        MojoExecution mojo = event.getMojoExecution();

        String mojoKey = project.getArtifactId() + ":" + mojo.getExecutionId() + ":" + mojo.getGoal();
        mojoStartTimes.put(mojoKey, MonotonicClock.now());

        machineBel.emitEvent(new JsonLine("mojo.started")
                .field("module", project.getName())
                .field("plugin", mojo.getArtifactId())
                .field("goal", mojo.getGoal())
                .field("phase", mojo.getLifecyclePhase())
                .field("executionId", mojo.getExecutionId())
                .build());
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        logMojoFinished(event, "mojo.succeeded");
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        MavenProject project = event.getProject();
        MojoExecution mojo = event.getMojoExecution();

        String mojoKey = project.getArtifactId() + ":" + mojo.getExecutionId() + ":" + mojo.getGoal();
        Instant start = mojoStartTimes.remove(mojoKey);

        JsonLine line = new JsonLine("mojo.failed")
                .field("module", project.getName())
                .field("plugin", mojo.getArtifactId())
                .field("goal", mojo.getGoal());
        if (start != null) {
            double duration = Duration.between(start, MonotonicClock.now()).toMillis() / 1000.0;
            line.field("duration", duration);
        }
        if (event.getException() != null) {
            line.field("error", event.getException().getMessage());
        }
        machineBel.emitEvent(line.build());
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        MavenProject project = event.getProject();
        MojoExecution mojo = event.getMojoExecution();

        machineBel.emitEvent(new JsonLine("mojo.skipped")
                .field("module", project.getName())
                .field("plugin", mojo.getArtifactId())
                .field("goal", mojo.getGoal())
                .build());
    }

    // ---- Fork lifecycle (machine mode emits these for completeness) ----

    @Override
    public void forkStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        MojoExecution mojo = event.getMojoExecution();

        machineBel.emitEvent(new JsonLine("fork.started")
                .field("module", project.getName())
                .field("plugin", mojo.getArtifactId())
                .field("goal", mojo.getGoal())
                .build());
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        machineBel.emitEvent(new JsonLine("fork.succeeded")
                .field("module", project.getName())
                .build());
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        MavenProject project = event.getProject();
        JsonLine line = new JsonLine("fork.failed").field("module", project.getName());
        if (event.getException() != null) {
            line.field("error", event.getException().getMessage());
        }
        machineBel.emitEvent(line.build());
    }

    // ---- Helpers ----

    private void logModuleFinished(ExecutionEvent event, String eventType) {
        MavenProject project = event.getProject();
        MavenSession session = event.getSession();
        BuildSummary summary = session.getResult().getBuildSummary(project);

        JsonLine line = new JsonLine(eventType).field("module", project.getName());
        if (summary != null) {
            line.field("duration", summary.getExecTime().toMillis() / 1000.0);
        }
        if ("module.failed".equals(eventType) && event.getException() != null) {
            line.field("error", event.getException().getMessage());
        }
        machineBel.emitEvent(line.build());
    }

    private void logMojoFinished(ExecutionEvent event, String eventType) {
        MavenProject project = event.getProject();
        MojoExecution mojo = event.getMojoExecution();

        String mojoKey = project.getArtifactId() + ":" + mojo.getExecutionId() + ":" + mojo.getGoal();
        Instant start = mojoStartTimes.remove(mojoKey);

        JsonLine line = new JsonLine(eventType)
                .field("module", project.getName())
                .field("plugin", mojo.getArtifactId())
                .field("goal", mojo.getGoal());
        if (start != null) {
            double duration = Duration.between(start, MonotonicClock.now()).toMillis() / 1000.0;
            line.field("duration", duration);
        }
        machineBel.emitEvent(line.build());
    }
}
