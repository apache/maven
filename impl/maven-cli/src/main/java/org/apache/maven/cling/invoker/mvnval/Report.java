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

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;

/**
 * Outcome of validating a single POM. {@code failure} is set instead of {@code problems} when the
 * file could not be validated at all, for instance because it does not exist.
 */
record Report(
        Path pom, List<ModelProblem> problems, @Nullable String failure) {

    Report {
        problems = List.copyOf(problems);
        if (failure != null && !problems.isEmpty()) {
            throw new IllegalArgumentException("a failed report carries no problems");
        }
    }

    static Report of(Path pom, List<ModelProblem> problems) {
        return new Report(pom, dedupe(problems), null);
    }

    static Report failed(Path pom, String failure) {
        return new Report(pom, List.of(), failure);
    }

    boolean clean() {
        return failure == null && problems.isEmpty();
    }

    boolean hasErrors() {
        return failure != null
                || problems.stream()
                        .anyMatch(problem -> problem.getSeverity() == BuilderProblem.Severity.ERROR
                                || problem.getSeverity() == BuilderProblem.Severity.FATAL);
    }

    /**
     * Returns the file a problem came from, or {@code null} when it came from this report's own
     * POM and naming it would only repeat what the caller printed.
     * <p>
     * The model builder reports the source as a {@code file:} URL while the rest of the output
     * uses paths, so the URL is converted back. A source that is not a file at all, such as a
     * {@code groupId:artifactId}, is passed through as it came.
     */
    @Nullable
    String sourceOf(ModelProblem problem) {
        String source = problem.getSource();
        if (source == null || source.isEmpty()) {
            return null;
        }
        Path path = toPath(source);
        if (path == null) {
            return source;
        }
        return path.toAbsolutePath().normalize().equals(pom.toAbsolutePath().normalize()) ? null : path.toString();
    }

    @Nullable
    private static Path toPath(String source) {
        try {
            Path path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            // Not made absolute: a source that is not a file parses as a relative path on Unix,
            // and prefixing it with the working directory would invent a file.
            return path.normalize();
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            return null;
        }
    }

    /**
     * Drops problems that repeat one already in the list. The file model and the raw model are
     * validated in turn and some checks sit in both, so a POM with {@code LATEST} for its parent
     * version is told about it twice. That is one thing wrong with the file, not two.
     */
    private static List<ModelProblem> dedupe(List<ModelProblem> problems) {
        Set<List<Object>> seen = new HashSet<>();
        return problems.stream()
                .filter(p -> seen.add(List.of(
                        p.getSeverity(),
                        String.valueOf(p.getMessage()),
                        String.valueOf(p.getSource()),
                        p.getLineNumber(),
                        p.getColumnNumber())))
                .toList();
    }
}
