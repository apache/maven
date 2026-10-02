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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.services.ModelProblem;

/**
 * Renders validation results as plain text, one problem per line.
 */
class TextReporter {

    private static final String INDENT = "  ";

    static void report(List<Report> reports, Path cwd, Consumer<String> out) {
        Path base = realPath(cwd);
        for (Report report : reports) {
            String pom = display(report.pom(), base);
            if (report.failure() != null) {
                out.accept(pom + ": " + report.failure());
            } else if (report.clean()) {
                out.accept(pom + ": no problems");
            } else {
                out.accept(pom + ":");
                for (ModelProblem problem : report.problems()) {
                    out.accept(line(problem, report));
                }
            }
        }
    }

    /**
     * The path as the caller would have typed it: relative to the working directory when the file
     * is under it, absolute otherwise, so a path outside the tree is never ambiguous. The JSON
     * document keeps the absolute path, since whoever reads it may not share this directory.
     */
    private static String display(Path pom, Path base) {
        return pom.startsWith(base) ? base.relativize(pom).toString() : pom.toString();
    }

    /**
     * Both paths have to be canonical before one can be said to sit under the other. On macOS the
     * working directory is routinely reached through a symbolic link, and comparing the link
     * against the resolved file gives a relative path that climbs out and back in again.
     */
    private static Path realPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private static String line(ModelProblem problem, Report report) {
        // A parser failure arrives as a message of several lines; the continuations are indented
        // one step deeper than the entry so it still reads as one.
        String message = String.valueOf(problem.getMessage())
                .lines()
                .collect(Collectors.joining(System.lineSeparator() + INDENT + INDENT));
        String text = INDENT + problem.getSeverity() + " " + message;
        String where = location(problem, report);
        return where.isEmpty() ? text : text + " @ " + where;
    }

    /**
     * Where the problem sits, as {@code file, line N, column M}, leaving out whatever is unknown.
     * The file is named only when it is not the one being reported on.
     * <p>
     * {@link ModelProblem#getLocation()} is no use here: {@code DefaultModelProblem}, which is
     * what the model builder produces, hardcodes it to an empty string.
     */
    private static String location(ModelProblem problem, Report report) {
        return Stream.of(
                        report.sourceOf(problem),
                        problem.getLineNumber() > 0 ? "line " + problem.getLineNumber() : null,
                        problem.getColumnNumber() > 0 ? "column " + problem.getColumnNumber() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }
}
