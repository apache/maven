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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.api.Session;
import org.apache.maven.api.spi.LifecycleParticipant;
import org.apache.maven.api.spi.LifecycleParticipantException;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifecycleParticipantDispatcherTest {

    @Test
    void invokesAllParticipantsAndAggregatesFailures() {
        List<String> callbacks = new ArrayList<>();
        LifecycleParticipantException firstFailure = new LifecycleParticipantException("first failure");
        LifecycleParticipantException secondFailure = new LifecycleParticipantException("second failure");

        MavenSession mavenSession = mock(MavenSession.class);
        Session session = mock(Session.class);
        when(mavenSession.getSession()).thenReturn(session);
        when(session.getData()).thenReturn(new MapBackedSessionData());

        LifecycleParticipantDispatcher.setParticipants(
                mavenSession,
                List.of(
                        participant("first", callbacks, firstFailure),
                        participant("second", callbacks, secondFailure),
                        participant("third", callbacks, null)));

        MavenExecutionException exception = assertThrows(
                MavenExecutionException.class, () -> LifecycleParticipantDispatcher.afterSessionEnd(mavenSession));

        assertEquals(List.of("first", "second", "third"), callbacks);
        assertSame(firstFailure, exception.getCause());
        assertEquals(List.of(secondFailure), List.of(exception.getCause().getSuppressed()));
    }

    private LifecycleParticipant participant(
            String name, List<String> callbacks, LifecycleParticipantException failure) {
        return new LifecycleParticipant() {
            @Override
            public void afterSessionEnd(Session session) throws LifecycleParticipantException {
                callbacks.add(name);
                if (failure != null) {
                    throw failure;
                }
            }
        };
    }
}
