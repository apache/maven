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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.services.ModelProblem;

/**
 * Renders validation results as plain text, one problem per line.
 */
class TextReporter implements Reporter {

    private static final String INDENT = "  ";

    @Override
    public void report(List<Report> reports, Consumer<String> out) {
        for (Report report : reports) {
            if (report.failure() != null) {
                out.accept(report.pom() + ": " + report.failure());
            } else if (report.clean()) {
                out.accept(report.pom() + ": no problems");
            } else {
                out.accept(report.pom() + ":");
                for (ModelProblem problem : report.problems()) {
                    out.accept(line(problem, report));
                }
            }
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
