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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.cling.event.MachineBuildEventListener.JsonLine;
import org.apache.maven.internal.impl.InternalMavenSession;

/**
 * Execution event logger for machine-readable output ({@code --console=machine}).
 * <p>
 * Emits one JSON line per lifecycle event to the shared
 * {@link MachineBuildEventListener#emitEvent(String)} writer. This logger
 * handles the {@link org.apache.maven.api.ExecutionListener} events:
 * session start/end, project start/success/failure/skip, and mojo
 * start/success/failure/skip.
 *
 * @since 4.1.0
 * @see MachineBuildEventListener
 */
public class MachineExecutionEventLogger extends AbstractExecutionEventLogger {

    private final MachineBuildEventListener machineBel;

    // Track mojo start times for duration calculation
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();

    // Reactor and outcome state
    private final AtomicInteger totalProjects = new AtomicInteger();
    private final AtomicInteger currentVisitedProjectCount = new AtomicInteger();
    private volatile Instant buildStartTime;

    public MachineExecutionEventLogger(MachineBuildEventListener machineBel) {
        this.machineBel = Objects.requireNonNull(machineBel, "machineBel cannot be null");
    }

    // ---- Session lifecycle ----

    @Override
    public void sessionStarted(ExecutionEvent event) {
        Session session = event.session();
        List<Project> allProjects = getAllProjects(session);
        List<Project> projects = (session != null && session.getProjects() != null) ? session.getProjects() : List.of();
        if (allProjects != null && !allProjects.isEmpty()) {
            totalProjects.set(allProjects.size());
            currentVisitedProjectCount.set(allProjects.size() - projects.size());
        } else {
            totalProjects.set(projects.size());
            currentVisitedProjectCount.set(0);
        }
        buildStartTime = MonotonicClock.now();

        JsonLine line = new JsonLine("build.started").field("projectCount", totalProjects.get());

        String goals = getGoals(session);
        if (goals != null && !goals.isEmpty()) {
            line.field("goals", goals);
        }

        List<String> profiles = getActiveProfiles(session);
        if (profiles != null && !profiles.isEmpty()) {
            line.field("profiles", String.join(",", profiles));
        }

        machineBel.emitEvent(line.build());
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        if (event.exception().isPresent()) {
            buildFailed = true;
        }
        String status = (buildFailed || failedCount.get() > 0) ? "FAILURE" : "SUCCESS";
        double duration = 0;
        if (buildStartTime != null) {
            duration = Duration.between(buildStartTime, MonotonicClock.now()).toMillis() / 1000.0;
        }

        machineBel.emitEvent(new JsonLine("build.finished")
                .field("status", status)
                .field("duration", duration)
                .field("total", totalProjects.get())
                .field("passed", passedCount.get())
                .field("failed", failedCount.get())
                .field("skipped", skippedCount.get())
                .build());
    }

    // ---- Module lifecycle ----

    @Override
    public void projectStarted(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        if (project == null) {
            return;
        }
        projectStartTimes.put(projectKey(project), MonotonicClock.now());
        int index = currentVisitedProjectCount.incrementAndGet();

        machineBel.emitEvent(new JsonLine("module.started")
                .field("module", getProjectDisplayName(project))
                .field("groupId", project.groupId())
                .field("artifactId", project.artifactId())
                .field("version", project.version())
                .field("index", index)
                .field("total", totalProjects.get())
                .build());
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        passedCount.incrementAndGet();
        logModuleFinished(event, "module.succeeded");
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        failedCount.incrementAndGet();
        buildFailed = true;
        logModuleFinished(event, "module.failed");
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        skippedCount.incrementAndGet();
        Project project = event.project().orElse(null);
        String name = getProjectDisplayName(project);
        machineBel.emitEvent(
                new JsonLine("module.skipped").field("module", name).build());
    }

    // ---- Mojo lifecycle ----

    @Override
    public void mojoStarted(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        MojoExecution mojo = event.mojoExecution().orElse(null);
        if (project == null || mojo == null) {
            return;
        }

        String mojoKey = mojoKey(project, mojo);
        mojoStartTimes.put(mojoKey, MonotonicClock.now());

        machineBel.emitEvent(new JsonLine("mojo.started")
                .field("module", getProjectDisplayName(project))
                .field("plugin", mojoPlugin(mojo))
                .field("goal", mojoGoal(mojo))
                .field("phase", mojoPhase(mojo))
                .field("executionId", mojoExecutionId(mojo))
                .build());
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        logMojoFinished(event, "mojo.succeeded");
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        MojoExecution mojo = event.mojoExecution().orElse(null);
        if (project == null || mojo == null) {
            return;
        }

        String mojoKey = mojoKey(project, mojo);
        Instant start = mojoStartTimes.remove(mojoKey);

        JsonLine line = new JsonLine("mojo.failed")
                .field("module", getProjectDisplayName(project))
                .field("plugin", mojoPlugin(mojo))
                .field("goal", mojoGoal(mojo));
        if (start != null) {
            double duration = Duration.between(start, MonotonicClock.now()).toMillis() / 1000.0;
            line.field("duration", duration);
        }
        event.exception().ifPresent(e -> line.field("error", e.getMessage()));
        machineBel.emitEvent(line.build());
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        MojoExecution mojo = event.mojoExecution().orElse(null);
        String moduleName = getProjectDisplayName(project);
        String pluginName = mojoPlugin(mojo);
        String goalName = mojoGoal(mojo);

        machineBel.emitEvent(new JsonLine("mojo.skipped")
                .field("module", moduleName)
                .field("plugin", pluginName)
                .field("goal", goalName)
                .build());
    }

    // ---- Fork lifecycle (machine mode emits these for completeness) ----

    @Override
    public void forkStarted(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        MojoExecution mojo = event.mojoExecution().orElse(null);
        String moduleName = getProjectDisplayName(project);
        String pluginName = mojoPlugin(mojo);
        String goalName = mojoGoal(mojo);

        machineBel.emitEvent(new JsonLine("fork.started")
                .field("module", moduleName)
                .field("plugin", pluginName)
                .field("goal", goalName)
                .build());
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        machineBel.emitEvent(new JsonLine("fork.succeeded")
                .field("module", getProjectDisplayName(project))
                .build());
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        Project project = event.project().orElse(null);
        JsonLine line = new JsonLine("fork.failed").field("module", getProjectDisplayName(project));
        event.exception().ifPresent(e -> line.field("error", e.getMessage()));
        machineBel.emitEvent(line.build());
    }

    // ---- Helpers ----

    private static String mojoKey(Project project, MojoExecution mojo) {
        return project.groupId() + ":" + project.artifactId() + ":" + mojoExecutionId(mojo) + ":" + mojoGoal(mojo);
    }

    private static String mojoPlugin(MojoExecution mojo) {
        if (mojo == null || mojo.plugin() == null) {
            return "unknown";
        }
        if (mojo.plugin().getDescriptor() != null
                && mojo.plugin().getDescriptor().getArtifactId() != null) {
            return mojo.plugin().getDescriptor().getArtifactId();
        }
        if (mojo.plugin().getModel() != null && mojo.plugin().getModel().getArtifactId() != null) {
            return mojo.plugin().getModel().getArtifactId();
        }
        return "unknown";
    }

    private static String mojoGoal(MojoExecution mojo) {
        return (mojo != null) ? mojo.goal() : "unknown";
    }

    private static String mojoPhase(MojoExecution mojo) {
        return (mojo != null) ? mojo.lifecyclePhase().orElse("") : "";
    }

    private static String mojoExecutionId(MojoExecution mojo) {
        return (mojo != null) ? mojo.executionId().orElse("") : "";
    }

    private void logModuleFinished(ExecutionEvent event, String eventType) {
        Project project = event.project().orElse(null);
        if (project == null) {
            return;
        }
        Duration recorded = projectDurations.remove(projectKey(project));
        double duration = 0;
        if (recorded != null) {
            duration = recorded.toMillis() / 1000.0;
        } else {
            Instant start = projectStartTimes.remove(projectKey(project));
            if (start != null) {
                duration = Duration.between(start, MonotonicClock.now()).toMillis() / 1000.0;
            }
        }

        JsonLine line = new JsonLine(eventType).field("module", getProjectDisplayName(project));
        if (duration > 0) {
            line.field("duration", duration);
        }
        if ("module.failed".equals(eventType)) {
            event.exception().ifPresent(e -> line.field("error", e.getMessage()));
        }
        machineBel.emitEvent(line.build());
    }

    private static String getGoals(Session session) {
        if (session == null) {
            return "";
        }
        if (session instanceof InternalMavenSession ims) {
            try {
                org.apache.maven.execution.MavenSession ms = ims.getMavenSession();
                if (ms != null) {
                    List<String> goals = ms.getGoals();
                    if (goals != null && !goals.isEmpty()) {
                        return String.join(" ", goals);
                    }
                    if (ms.getRequest() != null && ms.getRequest().getGoals() != null) {
                        return String.join(" ", ms.getRequest().getGoals());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (session instanceof ExecutionListenerAdapter.SessionAdapterHelper helper) {
            List<String> goals = helper.getGoals();
            if (goals != null && !goals.isEmpty()) {
                return String.join(" ", goals);
            }
        }
        return "";
    }

    private static List<String> getActiveProfiles(Session session) {
        if (session == null) {
            return Collections.emptyList();
        }
        if (session instanceof InternalMavenSession ims) {
            try {
                org.apache.maven.execution.MavenSession ms = ims.getMavenSession();
                if (ms != null && ms.getRequest() != null && ms.getRequest().getActiveProfiles() != null) {
                    return ms.getRequest().getActiveProfiles();
                }
            } catch (Exception ignored) {
            }
        }
        if (session instanceof ExecutionListenerAdapter.SessionAdapterHelper helper) {
            return helper.getActiveProfiles();
        }
        return Collections.emptyList();
    }

    private void logMojoFinished(ExecutionEvent event, String eventType) {
        Project project = event.project().orElse(null);
        MojoExecution mojo = event.mojoExecution().orElse(null);
        if (project == null || mojo == null) {
            return;
        }

        String mojoKey = mojoKey(project, mojo);
        Instant start = mojoStartTimes.remove(mojoKey);

        JsonLine line = new JsonLine(eventType)
                .field("module", getProjectDisplayName(project))
                .field("plugin", mojoPlugin(mojo))
                .field("goal", mojoGoal(mojo));
        if (start != null) {
            double duration = Duration.between(start, MonotonicClock.now()).toMillis() / 1000.0;
            line.field("duration", duration);
        }
        machineBel.emitEvent(line.build());
    }
}
