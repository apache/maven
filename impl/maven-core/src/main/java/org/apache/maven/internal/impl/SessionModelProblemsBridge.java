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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;

import static java.util.Objects.requireNonNull;

/**
 * Stores the legacy model-problem flag in session data so that derived sessions see the same state.
 */
public final class SessionModelProblemsBridge {

    private static final SessionData.Key<State> KEY = SessionData.key(State.class, SessionModelProblemsBridge.class);

    private SessionModelProblemsBridge() {}

    public static boolean hasModelProblems(Session session) {
        return getState(session).legacyFlag.get()
                || session.getModelProblemCollector().hasWarningProblems();
    }

    public static void setLegacyFlag(Session session, boolean value) {
        getState(session).legacyFlag.set(value);
    }

    private static State getState(Session session) {
        requireNonNull(session, "session");
        return session.getData().computeIfAbsent(KEY, State::new);
    }

    private static final class State {

        private final AtomicBoolean legacyFlag = new AtomicBoolean();
    }
}
