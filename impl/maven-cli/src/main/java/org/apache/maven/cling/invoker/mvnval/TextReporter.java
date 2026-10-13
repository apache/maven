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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.jline.MessageUtils;

/**
 * Renders validation results as plain text, one problem per line, with optional
 * source-context lines and ANSI color when the terminal supports it.
 */
class TextReporter {

    private static final String INDENT = "  ";
    private static final int MAX_LINE_WIDTH = 120;

    static void report(List<Report> reports, Path cwd, Consumer<String> out, int context, boolean color) {
        Path base = realPath(cwd);
        for (Report report : reports) {
            String pom = display(report.pom(), base);
            if (report.failure() != null) {
                out.accept(newBuilder(color).strong(pom + ":").build() + " " + report.failure());
            } else if (report.clean()) {
                out.accept(newBuilder(color).success(pom + ": no problems").build());
            } else {
                out.accept(newBuilder(color).strong(pom + ":").build());
                for (ModelProblem problem : report.problems()) {
                    out.accept(problemLine(problem, report, color));
                    if (context > 0 && problem.getLineNumber() > 0) {
                        contextLines(problem, report, context, color, out);
                    }
                }
            }
        }
    }

    /** Returns a color-aware or plain {@link MessageBuilder}. */
    private static MessageBuilder newBuilder(boolean color) {
        return color ? MessageUtils.builder() : new PlainMessageBuilder();
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

    private static String problemLine(ModelProblem problem, Report report, boolean color) {
        // A parser failure arrives as a message of several lines; the continuations are indented
        // one step deeper than the entry so it still reads as one.
        String message = String.valueOf(problem.getMessage())
                .lines()
                .collect(Collectors.joining(System.lineSeparator() + INDENT + INDENT));

        MessageBuilder b = newBuilder(color).a(INDENT);
        BuilderProblem.Severity severity = problem.getSeverity();
        if (severity == BuilderProblem.Severity.FATAL || severity == BuilderProblem.Severity.ERROR) {
            b.failure(severity.toString());
        } else {
            b.warning(severity.toString());
        }
        b.a(" ").a(message);

        String where = location(problem, report);
        if (!where.isEmpty()) {
            b.a(" @ ").a(where);
        }
        return b.build();
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

    /**
     * Reads the source file for the problem and emits the surrounding {@code context} lines,
     * with a {@code >} marker on the problem line and line numbers as a gutter.
     * <p>
     * Lines longer than {@value #MAX_LINE_WIDTH} characters are truncated with an ellipsis so
     * the output does not wrap on a standard terminal.
     */
    private static void contextLines(
            ModelProblem problem, Report report, int context, boolean color, Consumer<String> out) {
        Path source = resolveSourcePath(problem, report);
        if (source == null || !Files.isRegularFile(source)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }
        int total = lines.size();
        int problemLine = problem.getLineNumber(); // 1-based
        if (problemLine < 1 || problemLine > total) {
            return;
        }
        int first = Math.max(1, problemLine - context);
        int last = Math.min(total, problemLine + context);

        // Width of the line-number gutter: enough digits for the largest line number shown.
        int gutterWidth = Integer.toString(last).length();

        for (int i = first; i <= last; i++) {
            String text = lines.get(i - 1);
            if (text.length() > MAX_LINE_WIDTH) {
                text = text.substring(0, MAX_LINE_WIDTH - 1) + "\u2026";
            }
            boolean isProb = (i == problemLine);
            String marker = isProb ? ">" : " ";
            String num = String.format("%" + gutterWidth + "d", i);
            MessageBuilder b = newBuilder(color).a(INDENT).a(marker).a(" ");
            if (isProb) {
                b.strong(num + " | " + text);
            } else {
                b.a(num).a(" | ").a(text);
            }
            out.accept(b.build());
        }
    }

    /**
     * Resolves the path of the file containing the problem. Falls back to the report's own POM
     * when the problem has no source or the source cannot be converted to a path.
     */
    private static Path resolveSourcePath(ModelProblem problem, Report report) {
        String source = problem.getSource();
        if (source == null || source.isEmpty()) {
            return report.pom();
        }
        try {
            Path path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            path = path.normalize();
            return path.isAbsolute() ? path : report.pom().resolveSibling(path);
        } catch (IllegalArgumentException e) {
            return report.pom();
        }
    }

    /**
     * A plain {@link MessageBuilder} that produces text with no ANSI escapes.
     * Used when color is disabled.
     */
    private static final class PlainMessageBuilder implements MessageBuilder {

        private final StringBuilder buf = new StringBuilder();

        @Override
        public MessageBuilder style(String style) {
            return this;
        }

        @Override
        public MessageBuilder resetStyle() {
            return this;
        }

        @Override
        public MessageBuilder append(CharSequence cs) {
            buf.append(cs);
            return this;
        }

        @Override
        public MessageBuilder append(CharSequence cs, int start, int end) {
            buf.append(cs, start, end);
            return this;
        }

        @Override
        public MessageBuilder append(char c) {
            buf.append(c);
            return this;
        }

        @Override
        public MessageBuilder setLength(int length) {
            buf.setLength(length);
            return this;
        }

        @Override
        public String build() {
            return buf.toString();
        }
    }
}
