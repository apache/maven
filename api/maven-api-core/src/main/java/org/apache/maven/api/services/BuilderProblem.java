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
package org.apache.maven.api.services;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Describes a problem that was encountered during project building or
 * build execution. A problem can either be an exception that was thrown
 * or a simple string message. In addition, a problem carries a hint
 * about its source.
 * <p>
 * Since 4.1.0, problems can optionally carry a deduplication
 * {@link #getKey() key}, an actionable {@link #getSuggestion() suggestion},
 * and a {@link #getDocumentationUrl() documentation URL}. These fields
 * enable richer build reports and a deduplicated warning summary at
 * the end of the build.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
@ThreadSafe
public interface BuilderProblem {

    /**
     * Gets the hint about the source of the problem. While the syntax of this hint is unspecified and depends on the
     * creator of the problem, the general expectation is that the hint provides sufficient information to the user to
     * track the problem back to its origin. A concrete example for such a source hint can be the file path or URL from
     * which the settings were read.
     *
     * @return the hint about the source of the problem or an empty string if unknown, never {@code null}
     */
    @Nonnull
    String getSource();

    /**
     * Gets the one-based index of the line containing the problem. The line number should refer to some text file that
     * is given by {@link #getSource()}.
     *
     * @return the one-based index of the line containing the problem or a non-positive value if unknown
     */
    int getLineNumber();

    /**
     * Gets the one-based index of the column containing the problem. The column number should refer to some text file
     * that is given by {@link #getSource()}.
     *
     * @return the one-based index of the column containing the problem or non-positive value if unknown
     */
    int getColumnNumber();

    /**
     * Gets the location of the problem. The location is a user-friendly combination of the values from
     * {@link #getSource()}, {@link #getLineNumber()} and {@link #getColumnNumber()}. The exact syntax of the returned
     * value is undefined.
     *
     * @return the location of the problem, never {@code null}
     */
    @Nonnull
    String getLocation();

    /**
     * Gets the exception that caused this problem (if any).
     *
     * @return the exception that caused this problem or {@code null} if not applicable
     */
    @Nullable
    Exception getException();

    /**
     * Gets the message that describes this problem.
     *
     * @return the message describing this problem, never {@code null}
     */
    @Nonnull
    String getMessage();

    /**
     * Gets the severity level of this problem.
     *
     * @return the severity level of this problem, never {@code null}
     */
    @Nonnull
    Severity getSeverity();

    /**
     * Gets a stable deduplication key for this problem.
     * <p>
     * When multiple modules produce the same warning (e.g. a deprecated
     * POM element), reporting it with the same key allows the build report
     * to count occurrences instead of repeating the message. A key such as
     * {@code "deprecated-modules"} or {@code "compiler.unchecked:Foo.java:42"}
     * should be unique per logical problem but identical across modules
     * that encounter the same issue.
     * <p>
     * If this returns {@code null}, the problem is not deduplicated.
     *
     * @return the deduplication key, or {@code null} if not applicable
     * @since 4.1.0
     */
    @Nullable
    default String getKey() {
        return null;
    }

    /**
     * Gets an actionable suggestion for resolving this problem.
     * <p>
     * For example, a deprecation warning for {@code <modules>} might
     * suggest {@code "Use <subprojects> instead of <modules>"}.
     *
     * @return the suggestion text, or {@code null} if no suggestion is available
     * @since 4.1.0
     */
    @Nullable
    default String getSuggestion() {
        return null;
    }

    /**
     * Gets a URL pointing to documentation relevant to this problem.
     * <p>
     * For example, a warning about the deprecated {@code system} scope
     * might link to the Maven dependency scope migration guide.
     *
     * @return the documentation URL, or {@code null} if not available
     * @since 4.1.0
     */
    @Nullable
    default String getDocumentationUrl() {
        return null;
    }

    /**
     * Creates a new builder for constructing {@link BuilderProblem} instances.
     * <p>
     * This is the recommended way for plugins and extensions to create
     * structured problems to report via {@link DiagnosticReporter}.
     *
     * @return a new builder, never {@code null}
     * @since 4.1.0
     */
    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    /**
     * The different severity levels for a problem, in decreasing order.
     *
     * @since 4.0.0
     */
    @Experimental
    enum Severity {
        FATAL, //
        ERROR, //
        WARNING, //
        INFO //
    }

    /**
     * A builder for constructing immutable {@link BuilderProblem} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * BuilderProblem problem = BuilderProblem.builder()
     *     .severity(Severity.WARNING)
     *     .message("source/target value 8 is obsolete")
     *     .key("compiler:obsolete-source-target")
     *     .source("maven-compiler-plugin:3.15.0:compile")
     *     .suggestion("Update maven.compiler.source to 11 or higher")
     *     .documentationUrl("https://maven.apache.org/plugins/maven-compiler-plugin/")
     *     .build();
     * }</pre>
     *
     * @since 4.1.0
     */
    final class Builder {
        private String source = "";
        private int lineNumber = -1;
        private int columnNumber = -1;
        private Exception exception;
        private String message = "";
        private Severity severity = Severity.WARNING;
        private String key;
        private String suggestion;
        private String documentationUrl;

        Builder() {}

        @Nonnull
        public Builder source(@Nullable String source) {
            this.source = source != null ? source : "";
            return this;
        }

        @Nonnull
        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        @Nonnull
        public Builder columnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
            return this;
        }

        @Nonnull
        public Builder exception(@Nullable Exception exception) {
            this.exception = exception;
            return this;
        }

        @Nonnull
        public Builder message(@Nonnull String message) {
            this.message = message;
            return this;
        }

        @Nonnull
        public Builder severity(@Nonnull Severity severity) {
            this.severity = severity;
            return this;
        }

        @Nonnull
        public Builder key(@Nullable String key) {
            this.key = key;
            return this;
        }

        @Nonnull
        public Builder suggestion(@Nullable String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        @Nonnull
        public Builder documentationUrl(@Nullable String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        @Nonnull
        public BuilderProblem build() {
            return new DefaultProblem(
                    source, lineNumber, columnNumber, exception, message, severity, key, suggestion, documentationUrl);
        }

        /**
         * Immutable problem implementation returned by the builder.
         * This is intentionally package-private — callers use the
         * {@link BuilderProblem} interface.
         */
        @SuppressWarnings("checkstyle:ParameterNumber")
        private record DefaultProblem(
                String source,
                int lineNumber,
                int columnNumber,
                Exception exception,
                String message,
                Severity severity,
                String key,
                String suggestion,
                String documentationUrl)
                implements BuilderProblem {

            @Override
            @Nonnull
            public String getSource() {
                return source != null ? source : "";
            }

            @Override
            public int getLineNumber() {
                return lineNumber;
            }

            @Override
            public int getColumnNumber() {
                return columnNumber;
            }

            @Override
            @Nonnull
            public String getLocation() {
                StringBuilder buffer = new StringBuilder(256);
                if (source != null && !source.isEmpty()) {
                    buffer.append(source);
                }
                if (lineNumber > 0) {
                    if (!buffer.isEmpty()) {
                        buffer.append(", ");
                    }
                    buffer.append("line ").append(lineNumber);
                }
                if (columnNumber > 0) {
                    if (!buffer.isEmpty()) {
                        buffer.append(", ");
                    }
                    buffer.append("column ").append(columnNumber);
                }
                return buffer.toString();
            }

            @Override
            @Nullable
            public Exception getException() {
                return exception;
            }

            @Override
            @Nonnull
            public String getMessage() {
                return message != null ? message : "";
            }

            @Override
            @Nonnull
            public Severity getSeverity() {
                return severity != null ? severity : Severity.WARNING;
            }

            @Override
            @Nullable
            public String getKey() {
                return key;
            }

            @Override
            @Nullable
            public String getSuggestion() {
                return suggestion;
            }

            @Override
            @Nullable
            public String getDocumentationUrl() {
                return documentationUrl;
            }

            @Override
            public String toString() {
                StringBuilder buffer = new StringBuilder(128);
                buffer.append('[').append(getSeverity()).append("]");
                String msg = getMessage();
                if (!msg.isEmpty()) {
                    buffer.append(" ").append(msg);
                }
                String loc = getLocation();
                if (!loc.isEmpty()) {
                    buffer.append(" @ ").append(loc);
                }
                return buffer.toString();
            }
        }
    }
}
