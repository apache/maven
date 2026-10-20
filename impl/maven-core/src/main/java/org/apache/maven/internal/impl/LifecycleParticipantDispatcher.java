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
package org.apache.maven.internal.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.spi.LifecycleParticipant;
import org.apache.maven.api.spi.LifecycleParticipantException;
import org.apache.maven.api.spi.ProjectExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Bridges lifecycle callbacks from the legacy execution engine to the Maven 4 SPI.
 */
public final class LifecycleParticipantDispatcher {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final SessionData.Key<List<LifecycleParticipant>> PARTICIPANTS =
            (SessionData.Key) SessionData.key(List.class, LifecycleParticipant.class);

    private LifecycleParticipantDispatcher() {}

    public static void setParticipants(MavenSession session, Collection<LifecycleParticipant> participants) {
        session.getSession().getData().set(PARTICIPANTS, List.copyOf(participants));
    }

    public static void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        try {
            call(session, participant -> participant.afterProjectsRead(session.getSession()));
        } catch (LifecycleParticipantException e) {
            throw new MavenExecutionException("Lifecycle participant failed after projects were read", e);
        }
    }

    public static void beforeProjectExecution(
            MavenSession session, MavenProject project, List<org.apache.maven.plugin.MojoExecution> executionPlan)
            throws LifecycleExecutionException {
        if (!hasParticipants(session)) {
            return;
        }
        ProjectExecutionEvent event = event(session, project, executionPlan, null);
        try {
            call(session, participant -> participant.beforeProjectExecution(event));
        } catch (LifecycleParticipantException e) {
            throw new LifecycleExecutionException("Lifecycle participant failed before project execution", e);
        }
    }

    public static void afterProjectExecution(
            MavenSession session,
            MavenProject project,
            List<org.apache.maven.plugin.MojoExecution> executionPlan,
            Throwable failure)
            throws LifecycleExecutionException {
        if (!hasParticipants(session)) {
            return;
        }
        ProjectExecutionEvent event = event(session, project, executionPlan, failure);
        try {
            call(session, participant -> participant.afterProjectExecution(event));
        } catch (LifecycleParticipantException e) {
            throw new LifecycleExecutionException("Lifecycle participant failed after project execution", e);
        }
    }

    public static void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            call(session, participant -> participant.afterSessionEnd(session.getSession()));
        } catch (LifecycleParticipantException e) {
            throw new MavenExecutionException("Lifecycle participant failed at session end", e);
        }
    }

    private static ProjectExecutionEvent event(
            MavenSession session,
            MavenProject project,
            List<org.apache.maven.plugin.MojoExecution> executionPlan,
            Throwable failure) {
        InternalMavenSession apiSession = InternalMavenSession.from(session.getSession());
        Project apiProject = apiSession.getProject(project);
        List<MojoExecution> apiPlan = executionPlan.stream()
                .map(execution -> (MojoExecution) new DefaultMojoExecution(apiSession, execution))
                .toList();
        return new DefaultProjectExecutionEvent(apiSession, apiProject, apiPlan, failure);
    }

    private static void call(MavenSession session, ParticipantCall call) throws LifecycleParticipantException {
        List<LifecycleParticipant> participants = participants(session);
        if (participants == null) {
            return;
        }
        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        try {
            for (LifecycleParticipant participant : participants) {
                thread.setContextClassLoader(participant.getClass().getClassLoader());
                call.run(participant);
            }
        } finally {
            thread.setContextClassLoader(originalClassLoader);
        }
    }

    private static boolean hasParticipants(MavenSession session) {
        List<LifecycleParticipant> participants = participants(session);
        return participants != null && !participants.isEmpty();
    }

    private static List<LifecycleParticipant> participants(MavenSession session) {
        return session.getSession() != null ? session.getSession().getData().get(PARTICIPANTS) : null;
    }

    @FunctionalInterface
    private interface ParticipantCall {
        void run(LifecycleParticipant participant) throws LifecycleParticipantException;
    }

    private record DefaultProjectExecutionEvent(
            org.apache.maven.api.Session session, Project project, List<MojoExecution> executionPlan, Throwable cause)
            implements ProjectExecutionEvent {

        @Override
        public Optional<Throwable> failure() {
            return Optional.ofNullable(cause);
        }
    }
}
