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
package org.apache.maven.internal.build;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.apache.maven.api.services.BuilderProblem;

import static java.util.Objects.requireNonNull;

/**
 * Thread-safe collector for {@link BuilderProblem}s with deduplication support.
 * <p>
 * Problems with a non-null {@link BuilderProblem#getKey()} are deduplicated:
 * the first occurrence is stored, subsequent duplicates only increment the
 * counter. Problems without a key are always stored (up to the cap).
 * <p>
 * This implementation is safe for use from parallel module builds
 * ({@code -T}) and from any thread within a plugin execution.
 *
 * @since 4.1.0
 */
@Named
@Singleton
public final class DefaultDiagnosticCollector {

    /**
     * Maximum number of unique problems to store.
     * Protects against runaway plugins that produce unbounded problems.
     */
    static final int MAX_DIAGNOSTICS = 1000;

    /**
     * Preserves insertion order: key → first problem.
     * Using ConcurrentHashMap for thread safety; insertion order is tracked
     * separately in {@link #orderedKeys}.
     */
    private final Map<String, BuilderProblem> uniqueProblems = new ConcurrentHashMap<>();

    /** Counts per key (including the first occurrence). */
    private final Map<String, LongAdder> counts = new ConcurrentHashMap<>();

    /**
     * Insertion-order tracking. Synchronized on itself for ordered access.
     * The key list mirrors {@link #uniqueProblems} keys in insertion order.
     */
    private final List<String> orderedKeys = Collections.synchronizedList(new ArrayList<>());

    /** Counter for problems without a key, used to generate synthetic keys. */
    private final LongAdder noKeyCounter = new LongAdder();

    /**
     * Keys to suppress. Problems with a key in this set are silently dropped.
     * Configured via {@link #setSuppressedKeys(Set)}, typically from the
     * {@code maven.diagnostic.suppress} user property.
     */
    private volatile Set<String> suppressedKeys = Set.of();

    /**
     * Sets the keys to suppress. Problems with a matching key will be
     * silently dropped from {@link #report(BuilderProblem)}.
     * <p>
     * Supports both exact keys ({@code "deprecated-source-target"}) and
     * prefix matching with wildcard ({@code "auto:*"} to suppress all
     * auto-collected warnings from Maven 3 plugins).
     *
     * @param keys the set of keys to suppress; must not be null
     */
    public void setSuppressedKeys(Set<String> keys) {
        this.suppressedKeys = Set.copyOf(requireNonNull(keys, "keys"));
    }

    /**
     * Reports a problem.
     * <p>
     * If the problem has a non-null {@link BuilderProblem#getKey()} and a
     * problem with the same key has already been reported, the duplicate is
     * counted but not stored again.
     *
     * @param problem the problem to report
     */
    public void report(BuilderProblem problem) {
        requireNonNull(problem, "problem");
        String key = problem.getKey();

        // Problems without a key get a synthetic key for storage
        if (key == null) {
            noKeyCounter.increment();
            key = "__no_key__" + noKeyCounter.longValue();
        }

        // Check suppression: exact match or prefix wildcard (e.g. "auto:*")
        if (isSuppressed(key)) {
            return;
        }

        // Always increment the count
        counts.computeIfAbsent(key, k -> new LongAdder()).increment();

        // Store the first occurrence only (if within cap)
        if (uniqueProblems.putIfAbsent(key, problem) == null) {
            if (uniqueProblems.size() <= MAX_DIAGNOSTICS) {
                orderedKeys.add(key);
            } else {
                // Over cap — remove the entry we just added
                uniqueProblems.remove(key);
            }
        }
    }

    private boolean isSuppressed(String key) {
        Set<String> suppressed = this.suppressedKeys;
        if (suppressed.isEmpty()) {
            return false;
        }
        if (suppressed.contains(key)) {
            return true;
        }
        // Check prefix wildcards: "auto:*" matches "auto:Compiler:1a2b3c"
        for (String pattern : suppressed) {
            if (pattern.endsWith("*") && key.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all unique problems reported so far, in the order they
     * were first reported.
     *
     * @return an unmodifiable list of unique problems, never {@code null}
     */
    public List<BuilderProblem> getProblems() {
        List<BuilderProblem> result;
        synchronized (orderedKeys) {
            result = new ArrayList<>(orderedKeys.size());
            for (String key : orderedKeys) {
                BuilderProblem p = uniqueProblems.get(key);
                if (p != null) {
                    result.add(p);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns a deduplicated summary of all reported problems.
     * Each entry contains the problem and the number of times it was
     * reported (across all modules).
     *
     * @return an unmodifiable list of summaries, never {@code null}
     */
    public List<DefaultDiagnosticSummary> getSummary() {
        List<DefaultDiagnosticSummary> result;
        synchronized (orderedKeys) {
            result = new ArrayList<>(orderedKeys.size());
            for (String key : orderedKeys) {
                BuilderProblem p = uniqueProblems.get(key);
                LongAdder counter = counts.get(key);
                if (p != null && counter != null) {
                    result.add(new DefaultDiagnosticSummary(p, counter.intValue()));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns {@code true} if at least one problem with severity
     * {@link BuilderProblem.Severity#WARNING WARNING} or higher has been reported.
     */
    public boolean hasWarnings() {
        return hasAtSeverity(BuilderProblem.Severity.WARNING);
    }

    /**
     * Returns {@code true} if at least one problem with severity
     * {@link BuilderProblem.Severity#ERROR ERROR} has been reported.
     */
    public boolean hasErrors() {
        return hasAtSeverity(BuilderProblem.Severity.ERROR);
    }

    private boolean hasAtSeverity(BuilderProblem.Severity targetSeverity) {
        for (BuilderProblem p : uniqueProblems.values()) {
            // Severity enum is ordered most severe first: FATAL, ERROR, WARNING, INFO
            // A problem "has" the target severity if its ordinal is <= target ordinal
            if (p.getSeverity().ordinal() <= targetSeverity.ordinal()) {
                return true;
            }
        }
        return false;
    }
}
