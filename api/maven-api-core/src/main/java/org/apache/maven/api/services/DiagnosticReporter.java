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

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Service for reporting structured build diagnostics (warnings, errors,
 * informational messages) that will appear in the build report and in
 * the {@code mvnlog} output.
 * <p>
 * This is the recommended way for Maven 4 plugins and extensions to
 * report build problems with structured metadata (deduplication key,
 * actionable suggestion, documentation URL). Problems reported through
 * this service are:
 * <ul>
 *   <li>Deduplicated by {@link BuilderProblem#getKey() key} across modules</li>
 *   <li>Included in the JSON build report ({@code build-report-*.json})</li>
 *   <li>Shown in the warning summary at the end of the build</li>
 *   <li>Displayed with structured details by {@code mvnlog --diagnostics}</li>
 * </ul>
 * <p>
 * Plugins can inject this service via {@code @Inject} or retrieve it
 * from the session:
 * <pre>{@code
 * @Inject DiagnosticReporter diagnosticReporter;
 *
 * public void execute() {
 *     diagnosticReporter.report(BuilderProblem.builder()
 *         .severity(BuilderProblem.Severity.WARNING)
 *         .message("source/target value 8 is obsolete")
 *         .key("compiler:obsolete-source-target")
 *         .source("maven-compiler-plugin:3.15.0:compile")
 *         .suggestion("Update maven.compiler.source to 11 or higher")
 *         .documentationUrl("https://maven.apache.org/plugins/maven-compiler-plugin/")
 *         .build());
 * }
 * }</pre>
 *
 * @since 4.1.0
 * @see BuilderProblem#builder()
 */
@Experimental
@ThreadSafe
public interface DiagnosticReporter extends Service {

    /**
     * Reports a structured build problem.
     * <p>
     * If the problem has a non-null {@link BuilderProblem#getKey() key}
     * and a problem with the same key has already been reported, the
     * duplicate is counted but not stored again.
     *
     * @param problem the problem to report; must not be {@code null}
     */
    void report(@Nonnull BuilderProblem problem);

    /**
     * Convenience method to report a warning with all structured fields.
     *
     * @param message           the warning message
     * @param key               deduplication key, or {@code null}
     * @param source            source hint (e.g. plugin GAV), or {@code null}
     * @param suggestion        actionable fix suggestion, or {@code null}
     * @param documentationUrl  URL to relevant documentation, or {@code null}
     */
    default void warning(
            @Nonnull String message,
            @Nullable String key,
            @Nullable String source,
            @Nullable String suggestion,
            @Nullable String documentationUrl) {
        report(BuilderProblem.builder()
                .severity(BuilderProblem.Severity.WARNING)
                .message(message)
                .key(key)
                .source(source)
                .suggestion(suggestion)
                .documentationUrl(documentationUrl)
                .build());
    }

    /**
     * Convenience method to report a simple warning message.
     *
     * @param message the warning message
     */
    default void warning(@Nonnull String message) {
        warning(message, null, null, null, null);
    }
}
