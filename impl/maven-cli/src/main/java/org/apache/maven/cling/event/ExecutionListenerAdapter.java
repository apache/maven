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

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.ExecutionEventType;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultMojoExecution;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.project.MavenProject;

/**
 * Adapts a v4 {@link org.apache.maven.api.ExecutionListener} to the legacy
 * v3 {@link org.apache.maven.execution.ExecutionListener} interface.
 *
 * @since 4.1.0
 */
public class ExecutionListenerAdapter implements ExecutionListener {

    private final org.apache.maven.api.ExecutionListener delegate;

    public ExecutionListenerAdapter(org.apache.maven.api.ExecutionListener delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    public org.apache.maven.api.ExecutionListener getDelegate() {
        return delegate;
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        delegate.projectDiscoveryStarted(toApiEvent(event, ExecutionEventType.PROJECT_DISCOVERY_STARTED));
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        delegate.sessionStarted(toApiEvent(event, ExecutionEventType.SESSION_STARTED));
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        syncSessionResult(event);
        delegate.sessionEnded(toApiEvent(event, ExecutionEventType.SESSION_ENDED));
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        syncProjectDuration(event);
        delegate.projectSkipped(toApiEvent(event, ExecutionEventType.PROJECT_SKIPPED));
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        delegate.projectStarted(toApiEvent(event, ExecutionEventType.PROJECT_STARTED));
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        syncProjectDuration(event);
        delegate.projectSucceeded(toApiEvent(event, ExecutionEventType.PROJECT_SUCCEEDED));
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        syncProjectDuration(event);
        delegate.projectFailed(toApiEvent(event, ExecutionEventType.PROJECT_FAILED));
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        delegate.mojoSkipped(toApiEvent(event, ExecutionEventType.MOJO_SKIPPED));
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        delegate.mojoStarted(toApiEvent(event, ExecutionEventType.MOJO_STARTED));
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        delegate.mojoSucceeded(toApiEvent(event, ExecutionEventType.MOJO_SUCCEEDED));
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        delegate.mojoFailed(toApiEvent(event, ExecutionEventType.MOJO_FAILED));
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        delegate.forkStarted(toApiEvent(event, ExecutionEventType.FORK_STARTED));
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        delegate.forkSucceeded(toApiEvent(event, ExecutionEventType.FORK_SUCCEEDED));
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        delegate.forkFailed(toApiEvent(event, ExecutionEventType.FORK_FAILED));
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        delegate.forkedProjectStarted(toApiEvent(event, ExecutionEventType.FORKED_PROJECT_STARTED));
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        delegate.forkedProjectSucceeded(toApiEvent(event, ExecutionEventType.FORKED_PROJECT_SUCCEEDED));
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        delegate.forkedProjectFailed(toApiEvent(event, ExecutionEventType.FORKED_PROJECT_FAILED));
    }

    protected org.apache.maven.api.ExecutionEvent toApiEvent(ExecutionEvent event, ExecutionEventType type) {
        if (event == null) {
            return null;
        }
        Session v4Session = null;
        InternalMavenSession internalSession = null;
        if (event.getSession() != null) {
            try {
                if (event.getSession().getSession() instanceof InternalMavenSession ims) {
                    internalSession = ims;
                    v4Session = ims;
                } else if (event.getSession().getSession() != null) {
                    v4Session = event.getSession().getSession();
                }
            } catch (Exception ignored) {
            }
            if (v4Session == null) {
                v4Session = createSessionAdapter(event.getSession());
            }
        }

        Project v4Project = null;
        if (event.getProject() != null) {
            if (internalSession != null) {
                try {
                    v4Project = internalSession.getProject(event.getProject());
                } catch (Exception ignored) {
                }
            }
            if (v4Project == null) {
                v4Project = new ProjectAdapter(event.getProject());
            }
        }

        MojoExecution v4Mojo = null;
        if (event.getMojoExecution() != null) {
            if (internalSession != null) {
                try {
                    v4Mojo = new DefaultMojoExecution(internalSession, event.getMojoExecution());
                } catch (Exception ignored) {
                }
            }
            if (v4Mojo == null) {
                v4Mojo = createMojoExecutionAdapter(event.getMojoExecution());
            }
        }

        Exception exception = event.getException();
        return new SimpleExecutionEvent(type, v4Session, v4Project, v4Mojo, exception);
    }

    static class SimpleExecutionEvent implements org.apache.maven.api.ExecutionEvent {
        private final ExecutionEventType type;
        private final Session session;
        private final Project project;
        private final MojoExecution mojoExecution;
        private final Exception exception;

        SimpleExecutionEvent(
                ExecutionEventType type,
                Session session,
                Project project,
                MojoExecution mojoExecution,
                Exception exception) {
            this.type = type;
            this.session = session;
            this.project = project;
            this.mojoExecution = mojoExecution;
            this.exception = exception;
        }

        @Override
        public ExecutionEventType type() {
            return type;
        }

        @Override
        public Session session() {
            return session;
        }

        @Override
        public Optional<Project> project() {
            return Optional.ofNullable(project);
        }

        @Override
        public Optional<MojoExecution> mojoExecution() {
            return Optional.ofNullable(mojoExecution);
        }

        @Override
        public Optional<Exception> exception() {
            return Optional.ofNullable(exception);
        }
    }

    static class ProjectAdapter implements Project {
        private final MavenProject project;

        ProjectAdapter(MavenProject project) {
            this.project = Objects.requireNonNull(project, "project cannot be null");
        }

        public MavenProject getMavenProject() {
            return project;
        }

        @Override
        public String groupId() {
            return project.getGroupId();
        }

        @Override
        public String artifactId() {
            String aid = project.getArtifactId();
            return aid != null ? aid : (project.getName() != null ? project.getName() : "");
        }

        @Override
        public String version() {
            return project.getVersion();
        }

        @Override
        public Packaging packaging() {
            return null;
        }

        @Override
        public Model model() {
            if (project.getModel() != null && project.getModel().getDelegate() != null) {
                return project.getModel().getDelegate();
            }
            return Model.newBuilder()
                    .name(project.getName())
                    .artifactId(project.getArtifactId() != null ? project.getArtifactId() : project.getName())
                    .groupId(project.getGroupId())
                    .version(project.getVersion())
                    .build();
        }

        @Override
        public Path pomPath() {
            return project.getFile() != null ? project.getFile().toPath() : null;
        }

        @Override
        public Path basedir() {
            return project.getBasedir() != null ? project.getBasedir().toPath() : null;
        }

        @Override
        public List<ProducedArtifact> artifacts() {
            return Collections.emptyList();
        }

        @Override
        public List<DependencyCoordinates> dependencies() {
            return Collections.emptyList();
        }

        @Override
        public List<DependencyCoordinates> managedDependencies() {
            return Collections.emptyList();
        }

        @Override
        public boolean isTopProject() {
            return project.isExecutionRoot();
        }

        @Override
        public boolean isRootProject() {
            return false;
        }

        @Override
        public Path rootDirectory() {
            return basedir();
        }

        @Override
        public Optional<Project> parent() {
            return Optional.empty();
        }

        @Override
        public List<Profile> declaredProfiles() {
            return model().getProfiles();
        }

        @Override
        public List<Profile> effectiveProfiles() {
            return declaredProfiles();
        }

        @Override
        public List<Profile> declaredActiveProfiles() {
            return Collections.emptyList();
        }

        @Override
        public List<Profile> effectiveActiveProfiles() {
            return Collections.emptyList();
        }
    }

    public interface SessionAdapterHelper {
        List<Project> getAllProjects();

        List<String> getGoals();

        List<String> getActiveProfiles();

        MavenSession getMavenSession();
    }

    static Session createSessionAdapter(MavenSession mavenSession) {
        if (mavenSession == null) {
            return null;
        }
        return (Session) Proxy.newProxyInstance(
                Session.class.getClassLoader(),
                new Class<?>[] {Session.class, SessionAdapterHelper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getMavenSession".equals(name)) {
                        return mavenSession;
                    }
                    if ("getProjects".equals(name) || "projects".equals(name)) {
                        List<MavenProject> projects = mavenSession.getProjects();
                        if (projects == null) {
                            return Collections.emptyList();
                        }
                        List<Project> result = new ArrayList<>(projects.size());
                        for (MavenProject mp : projects) {
                            result.add(new ProjectAdapter(mp));
                        }
                        return result;
                    }
                    if ("getAllProjects".equals(name) || "allProjects".equals(name)) {
                        List<MavenProject> projects = mavenSession.getAllProjects();
                        if (projects == null) {
                            return Collections.emptyList();
                        }
                        List<Project> result = new ArrayList<>(projects.size());
                        for (MavenProject mp : projects) {
                            result.add(new ProjectAdapter(mp));
                        }
                        return result;
                    }
                    if ("getSystemProperties".equals(name) || "systemProperties".equals(name)) {
                        Map<String, String> map = new LinkedHashMap<>();
                        if (mavenSession.getSystemProperties() != null) {
                            for (String key : mavenSession.getSystemProperties().stringPropertyNames()) {
                                map.put(key, mavenSession.getSystemProperties().getProperty(key));
                            }
                        }
                        return Collections.unmodifiableMap(map);
                    }
                    if ("getStartTime".equals(name) || "startTime".equals(name)) {
                        return mavenSession.getStartTime() != null
                                ? mavenSession.getStartTime().toInstant()
                                : Instant.now();
                    }
                    if ("getDegreeOfConcurrency".equals(name) || "degreeOfConcurrency".equals(name)) {
                        return mavenSession.getRequest() != null
                                ? mavenSession.getRequest().getDegreeOfConcurrency()
                                : 1;
                    }
                    if ("getRootDirectory".equals(name) || "rootDirectory".equals(name)) {
                        if (mavenSession.getTopLevelProject() != null
                                && mavenSession.getTopLevelProject().getBasedir() != null) {
                            return mavenSession
                                    .getTopLevelProject()
                                    .getBasedir()
                                    .toPath();
                        }
                        return Path.of(".");
                    }
                    if ("getTopDirectory".equals(name) || "topDirectory".equals(name)) {
                        if (mavenSession.getTopLevelProject() != null
                                && mavenSession.getTopLevelProject().getBasedir() != null) {
                            return mavenSession
                                    .getTopLevelProject()
                                    .getBasedir()
                                    .toPath();
                        }
                        return Path.of(".");
                    }
                    if ("getGoals".equals(name) || "goals".equals(name)) {
                        if (mavenSession.getGoals() != null
                                && !mavenSession.getGoals().isEmpty()) {
                            return mavenSession.getGoals();
                        }
                        if (mavenSession.getRequest() != null
                                && mavenSession.getRequest().getGoals() != null) {
                            return mavenSession.getRequest().getGoals();
                        }
                        return Collections.emptyList();
                    }
                    if ("getActiveProfiles".equals(name) || "activeProfiles".equals(name)) {
                        if (mavenSession.getRequest() != null
                                && mavenSession.getRequest().getActiveProfiles() != null) {
                            return mavenSession.getRequest().getActiveProfiles();
                        }
                        return Collections.emptyList();
                    }
                    if ("toString".equals(name)) {
                        return "SessionAdapter[" + mavenSession + "]";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == (args != null && args.length > 0 ? args[0] : null);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) {
                        return false;
                    }
                    if (returnType.equals(int.class)) {
                        return 0;
                    }
                    if (returnType.equals(long.class)) {
                        return 0L;
                    }
                    if (returnType.equals(Optional.class)) {
                        return Optional.empty();
                    }
                    if (returnType.equals(List.class)) {
                        return Collections.emptyList();
                    }
                    if (returnType.equals(Map.class)) {
                        return Collections.emptyMap();
                    }
                    return null;
                });
    }

    static MojoExecution createMojoExecutionAdapter(org.apache.maven.plugin.MojoExecution mojo) {
        if (mojo == null) {
            return null;
        }
        return (MojoExecution) Proxy.newProxyInstance(
                MojoExecution.class.getClassLoader(), new Class<?>[] {MojoExecution.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    if ("goal".equals(name)) {
                        return mojo.getGoal();
                    }
                    if ("executionId".equals(name)) {
                        return Optional.ofNullable(mojo.getExecutionId());
                    }
                    if ("lifecyclePhase".equals(name)) {
                        return Optional.ofNullable(mojo.getLifecyclePhase());
                    }
                    if ("plugin".equals(name)) {
                        return (Plugin) Proxy.newProxyInstance(
                                Plugin.class.getClassLoader(),
                                new Class<?>[] {Plugin.class},
                                (pProxy, pMethod, pArgs) -> {
                                    if ("getDescriptor".equals(pMethod.getName())) {
                                        return org.apache.maven.api.plugin.descriptor.PluginDescriptor.newBuilder()
                                                .groupId(mojo.getGroupId())
                                                .artifactId(mojo.getArtifactId())
                                                .version(mojo.getVersion())
                                                .build();
                                    }
                                    if ("getModel".equals(pMethod.getName())) {
                                        return org.apache.maven.api.model.Plugin.newBuilder()
                                                .groupId(mojo.getGroupId())
                                                .artifactId(mojo.getArtifactId())
                                                .version(mojo.getVersion())
                                                .build();
                                    }
                                    return null;
                                });
                    }
                    if ("toString".equals(name)) {
                        return "MojoExecutionAdapter[" + mojo + "]";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == (args != null && args.length > 0 ? args[0] : null);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(Optional.class)) {
                        return Optional.empty();
                    }
                    return null;
                });
    }

    private void syncProjectDuration(ExecutionEvent event) {
        if (event == null || event.getSession() == null || event.getProject() == null) {
            return;
        }
        if (event.getSession().getResult() != null) {
            BuildSummary bs = event.getSession().getResult().getBuildSummary(event.getProject());
            if (bs != null && bs.getTime() > 0) {
                Duration dur = Duration.ofMillis(bs.getTime());
                String key = projectKey(event.getProject());
                if (delegate instanceof PlainExecutionEventLogger pel) {
                    pel.recordProjectDuration(key, dur);
                } else if (delegate instanceof RichExecutionEventLogger rel) {
                    rel.recordProjectDuration(key, dur);
                } else if (delegate instanceof MachineExecutionEventLogger mel) {
                    mel.recordProjectDuration(key, dur);
                }
            }
        }
    }

    private void syncSessionResult(ExecutionEvent event) {
        if (event == null || event.getSession() == null || event.getSession().getResult() == null) {
            return;
        }
        MavenExecutionResult res = event.getSession().getResult();
        boolean hasExceptions = res.hasExceptions();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        List<MavenProject> projects = event.getSession().getProjects();
        if (projects != null) {
            for (MavenProject p : projects) {
                BuildSummary summary = res.getBuildSummary(p);
                if (summary instanceof BuildSuccess) {
                    passed++;
                } else if (summary instanceof BuildFailure) {
                    failed++;
                } else {
                    skipped++;
                }
            }
        }
        if (delegate instanceof PlainExecutionEventLogger pel) {
            pel.syncResult(hasExceptions, passed, failed, skipped);
        } else if (delegate instanceof RichExecutionEventLogger rel) {
            rel.syncResult(hasExceptions, passed, failed, skipped);
        } else if (delegate instanceof MachineExecutionEventLogger mel) {
            mel.syncResult(hasExceptions, passed, failed, skipped);
        }
    }

    private static String projectKey(MavenProject project) {
        String gid = project.getGroupId();
        String aid = project.getArtifactId();
        if (aid == null) {
            aid = project.getName() != null ? project.getName() : "";
        }
        return (gid != null ? gid : "") + ":" + aid;
    }
}
