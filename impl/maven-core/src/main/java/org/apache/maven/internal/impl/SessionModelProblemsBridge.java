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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;

import static java.util.Objects.requireNonNull;

/**
 * Bridges native model problems to the legacy model-building API.
 */
public final class SessionModelProblemsBridge {

    private static final SessionData.Key<State> KEY = SessionData.key(State.class, SessionModelProblemsBridge.class);

    private SessionModelProblemsBridge() {}

    public static List<ModelProblem> getModelProblems(Session session) {
        ProblemCollector<org.apache.maven.api.services.ModelProblem> collector =
                requireNonNull(session, "session").getModelProblemCollector();
        List<ModelProblem> problems = new ArrayList<>();
        if (collector.problemsOverflow()) {
            problems.add(new DefaultModelProblem(
                    "Too many model problems reported (listed problems are just a subset of reported problems)",
                    ModelProblem.Severity.WARNING,
                    null,
                    (String) null,
                    -1,
                    -1,
                    null,
                    null));
        }
        problems.addAll(getState(session).legacyProblems.get());
        collector.problems().map(SessionModelProblemsBridge::toLegacy).forEach(problems::add);
        return Collections.unmodifiableList(problems);
    }

    public static void setLegacyModelProblems(Session session, List<ModelProblem> problems) {
        getState(session)
                .legacyProblems
                .set(Collections.unmodifiableList(new ArrayList<>(requireNonNull(problems, "problems"))));
    }

    private static State getState(Session session) {
        requireNonNull(session, "session");
        return session.getData().computeIfAbsent(KEY, State::new);
    }

    private static ModelProblem toLegacy(org.apache.maven.api.services.ModelProblem problem) {
        return new DefaultModelProblem(
                problem.getMessage(),
                ModelProblem.Severity.valueOf(problem.getSeverity().name()),
                ModelProblem.Version.valueOf(problem.getVersion().name()),
                problem.getSource(),
                problem.getLineNumber(),
                problem.getColumnNumber(),
                problem.getModelId(),
                problem.getException());
    }

    private static final class State {

        private final AtomicReference<List<ModelProblem>> legacyProblems =
                new AtomicReference<>(Collections.emptyList());
    }
}
