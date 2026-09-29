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
package org.apache.maven.cling.invoker.mvnval;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.ModelBuilder.ModelBuilderSession;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderRequest.RequestType;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.Sources;

/**
 * How far the model builder runs before the verdict is taken.
 */
enum ValidationMode {
    /**
     * Reads the file and raw models only. Resolves no parent and opens no connection, so it works
     * offline and on a POM whose parent is not published yet, at the cost of the checks that need
     * an effective model.
     */
    RAW {
        @Override
        List<ModelProblem> problemsFor(ModelBuilderSession builderSession, Session session, Path pom)
                throws ModelBuilderException {
            return rawProblems(builderSession, session, pom);
        }
    },

    /**
     * Everything {@code raw} checks, plus what needs the effective model: parents and imported
     * boms are resolved, so a dependency whose version comes from the parent is checked too.
     */
    EFFECTIVE {
        @Override
        List<ModelProblem> problemsFor(ModelBuilderSession builderSession, Session session, Path pom)
                throws ModelBuilderException {
            // Raw first, and on its own request. The effective build reports nothing about the
            // reactor, so a subproject this POM names but does not have passes unnoticed through
            // build() alone, while mvn refuses to read the project.
            List<ModelProblem> problems = rawProblems(builderSession, session, pom);
            if (problems.stream().anyMatch(problem -> problem.getSeverity() == ModelProblem.Severity.ERROR)) {
                // Nothing is gained by resolving parents for a POM that is already rejected, and
                // it would go to the network to say so.
                return problems;
            }
            List<ModelProblem> all = new ArrayList<>(problems);
            // No problem is reported twice because the builder caches the file and raw models per
            // source, so this request hits that cache and re-runs neither check. Partition that
            // cache by request type and every raw problem would appear twice.
            try {
                // Completes the model: inheritance, interpolation, then validateEffectiveModel. It
                // throws once it has collected an error, so throwing is the ordinary path here.
                all.addAll(problemsOf(builderSession.build(request(RequestType.BUILD_EFFECTIVE, session, pom))));
                return all;
            } catch (ModelBuilderException e) {
                // Carry the raw warnings out with the exception's own problems. Letting it fly
                // would report only what the effective pass saw and lose the rest.
                ModelBuilderResult result = e.getResult();
                if (result == null) {
                    throw e;
                }
                all.addAll(problemsOf(result));
                return all;
            }
        }
    };

    /** Collects the problems this mode can see, in the order the model builder reports them. */
    abstract List<ModelProblem> problemsFor(ModelBuilderSession builderSession, Session session, Path pom)
            throws ModelBuilderException;

    /**
     * The raw pass. {@code BUILD_PROJECT} is what makes the builder look for the parent beside the
     * POM and check its {@code relativePath}; it does not make this a build, since {@code validate}
     * stops before inheritance.
     */
    private static List<ModelProblem> rawProblems(ModelBuilderSession builderSession, Session session, Path pom)
            throws ModelBuilderException {
        return problemsOf(builderSession.validate(request(RequestType.BUILD_PROJECT, session, pom)));
    }

    static List<ModelProblem> problemsOf(ModelBuilderResult result) {
        return result.getProblemCollector().problems().toList();
    }

    private static ModelBuilderRequest request(RequestType type, Session session, Path pom) {
        return ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(pom))
                .requestType(type)
                .build();
    }

    static ValidationMode parse(String name) {
        return Arrays.stream(values())
                .filter(mode -> mode.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown mode '" + name + "', expected one of: "
                        + Arrays.stream(values())
                                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(", "))));
    }
}
