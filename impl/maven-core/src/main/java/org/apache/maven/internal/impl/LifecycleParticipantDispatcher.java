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

import org.apache.maven.MavenExecutionException;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.spi.LifecycleParticipant;
import org.apache.maven.api.spi.LifecycleParticipantException;
import org.apache.maven.execution.MavenSession;

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

    private static void call(MavenSession session, ParticipantCall call) throws LifecycleParticipantException {
        List<LifecycleParticipant> participants = participants(session);
        if (participants == null) {
            return;
        }
        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        LifecycleParticipantException failure = null;
        try {
            for (LifecycleParticipant participant : participants) {
                thread.setContextClassLoader(participant.getClass().getClassLoader());
                try {
                    call.run(participant);
                } catch (LifecycleParticipantException | RuntimeException e) {
                    LifecycleParticipantException lpe =
                            e instanceof LifecycleParticipantException ex ? ex : new LifecycleParticipantException(e);
                    if (failure == null) {
                        failure = lpe;
                    } else {
                        failure.addSuppressed(lpe);
                    }
                }
            }
        } finally {
            thread.setContextClassLoader(originalClassLoader);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static List<LifecycleParticipant> participants(MavenSession session) {
        return session.getSession() != null ? session.getSession().getData().get(PARTICIPANTS) : null;
    }

    @FunctionalInterface
    private interface ParticipantCall {
        void run(LifecycleParticipant participant) throws LifecycleParticipantException;
    }
}
