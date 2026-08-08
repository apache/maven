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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.maven.api.services.BuilderProblem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDiagnosticCollectorTest {

    private DefaultDiagnosticCollector collector;

    @BeforeEach
    void setUp() {
        collector = new DefaultDiagnosticCollector();
    }

    private static BuilderProblem warning(String key, String message, String source) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.WARNING)
                .key(key)
                .build();
    }

    private static BuilderProblem error(String key, String message, String source) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.ERROR)
                .key(key)
                .build();
    }

    private static BuilderProblem info(String key, String message, String source) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.INFO)
                .key(key)
                .build();
    }

    @Test
    void testEmptyCollector() {
        assertTrue(collector.getProblems().isEmpty());
        assertTrue(collector.getSummary().isEmpty());
        assertFalse(collector.hasWarnings());
        assertFalse(collector.hasErrors());
    }

    @Test
    void testSingleWarning() {
        BuilderProblem p = warning("deprecated-source", "source 8 is deprecated", "compiler:3.15.0");
        collector.report(p);

        assertEquals(1, collector.getProblems().size());
        assertEquals("deprecated-source", collector.getProblems().get(0).getKey());
        assertTrue(collector.hasWarnings());
        assertFalse(collector.hasErrors());
    }

    @Test
    void testSingleError() {
        BuilderProblem p = error("compilation-failure", "3 errors found", "compiler:3.15.0");
        collector.report(p);

        assertEquals(1, collector.getProblems().size());
        assertTrue(collector.hasWarnings()); // errors are >= WARNING severity
        assertTrue(collector.hasErrors());
    }

    @Test
    void testInfoDoesNotCountAsWarningOrError() {
        BuilderProblem p = info("build-summary", "Build completed", "reactor");
        collector.report(p);

        assertEquals(1, collector.getProblems().size());
        assertFalse(collector.hasWarnings());
        assertFalse(collector.hasErrors());
    }

    @Test
    void testDeduplicationByKey() {
        BuilderProblem d1 = warning("deprecated-source", "source 8 is deprecated", "module-a");
        BuilderProblem d2 = warning("deprecated-source", "source 8 is deprecated", "module-b");
        BuilderProblem d3 = warning("deprecated-source", "source 8 is deprecated", "module-c");

        collector.report(d1);
        collector.report(d2);
        collector.report(d3);

        // Only one unique problem
        assertEquals(1, collector.getProblems().size());
        assertEquals("deprecated-source", collector.getProblems().get(0).getKey());

        // But the summary shows count = 3
        List<DefaultDiagnosticSummary> summary = collector.getSummary();
        assertEquals(1, summary.size());
        assertEquals(3, summary.get(0).count());
    }

    @Test
    void testMultipleDistinctProblems() {
        collector.report(warning("deprecated-source", "source 8 is deprecated", "compiler"));
        collector.report(warning("unused-dep", "unused dependency: guava", "dependency"));
        collector.report(error("test-failure", "2 tests failed", "surefire"));

        assertEquals(3, collector.getProblems().size());
        assertTrue(collector.hasWarnings());
        assertTrue(collector.hasErrors());

        List<DefaultDiagnosticSummary> summary = collector.getSummary();
        assertEquals(3, summary.size());
        // Each reported once
        for (DefaultDiagnosticSummary s : summary) {
            assertEquals(1, s.count());
        }
    }

    @Test
    void testInsertionOrderPreserved() {
        collector.report(warning("c-warning", "third", null));
        collector.report(warning("a-warning", "first", null));
        collector.report(warning("b-warning", "second", null));

        List<BuilderProblem> problems = collector.getProblems();
        assertEquals("c-warning", problems.get(0).getKey());
        assertEquals("a-warning", problems.get(1).getKey());
        assertEquals("b-warning", problems.get(2).getKey());
    }

    @Test
    void testProblemsListIsUnmodifiable() {
        collector.report(warning("test", "test", null));
        List<BuilderProblem> problems = collector.getProblems();

        try {
            problems.add(warning("another", "another", null));
            // Should not reach here
            assertFalse(true, "Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    void testSummaryListIsUnmodifiable() {
        collector.report(warning("test", "test", null));
        List<DefaultDiagnosticSummary> summary = collector.getSummary();

        try {
            summary.add(new DefaultDiagnosticSummary(warning("x", "x", null), 1));
            assertFalse(true, "Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    void testFullProblemFields() {
        BuilderProblem p = BuilderProblem.builder()
                .source("maven-compiler-plugin:3.15.0:compile")
                .lineNumber(42)
                .columnNumber(15)
                .message("unchecked cast from Object to List<String>")
                .severity(BuilderProblem.Severity.WARNING)
                .key("unchecked-cast")
                .suggestion("Add @SuppressWarnings(\"unchecked\") or use a type-safe alternative")
                .documentationUrl("https://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html")
                .build();

        collector.report(p);

        BuilderProblem stored = collector.getProblems().get(0);
        assertEquals("unchecked-cast", stored.getKey());
        assertEquals(BuilderProblem.Severity.WARNING, stored.getSeverity());
        assertEquals("unchecked cast from Object to List<String>", stored.getMessage());
        assertEquals("maven-compiler-plugin:3.15.0:compile", stored.getSource());
        assertEquals(42, stored.getLineNumber());
        assertEquals(15, stored.getColumnNumber());
        assertEquals("Add @SuppressWarnings(\"unchecked\") or use a type-safe alternative", stored.getSuggestion());
        assertEquals(
                "https://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html", stored.getDocumentationUrl());
    }

    // ---- Suppression tests ----

    @Test
    void testSuppressionByExactKey() {
        collector.setSuppressedKeys(Set.of("deprecated-source"));

        collector.report(warning("deprecated-source", "source 8 is deprecated", "compiler"));
        collector.report(warning("unused-dep", "unused dependency: guava", "dependency"));

        assertEquals(1, collector.getProblems().size());
        assertEquals("unused-dep", collector.getProblems().get(0).getKey());
    }

    @Test
    void testSuppressionByPrefixWildcard() {
        collector.setSuppressedKeys(Set.of("auto:*"));

        collector.report(warning("auto:Compiler:1a2b3c", "unchecked cast", "compiler"));
        collector.report(warning("auto:Surefire:4d5e6f", "deprecated API", "surefire"));
        collector.report(warning("explicit-key", "some warning", "plugin"));

        assertEquals(1, collector.getProblems().size());
        assertEquals("explicit-key", collector.getProblems().get(0).getKey());
    }

    @Test
    void testSuppressionMultipleKeys() {
        collector.setSuppressedKeys(Set.of("key-a", "key-b"));

        collector.report(warning("key-a", "warning a", null));
        collector.report(warning("key-b", "warning b", null));
        collector.report(warning("key-c", "warning c", null));

        assertEquals(1, collector.getProblems().size());
        assertEquals("key-c", collector.getProblems().get(0).getKey());
    }

    @Test
    void testSuppressionDoesNotAffectCounts() {
        collector.setSuppressedKeys(Set.of("suppressed"));

        // Report suppressed key — should be silently dropped, no count
        collector.report(warning("suppressed", "suppressed warning", null));
        collector.report(warning("kept", "kept warning", null));

        assertEquals(1, collector.getSummary().size());
        assertEquals("kept", collector.getSummary().get(0).problem().getKey());
        assertEquals(1, collector.getSummary().get(0).count());
    }

    @Test
    void testEmptySuppressionSetAllowsAll() {
        collector.setSuppressedKeys(Set.of());

        collector.report(warning("key-a", "warning a", null));
        collector.report(warning("key-b", "warning b", null));

        assertEquals(2, collector.getProblems().size());
    }

    @Test
    void testConcurrentReporting() throws Exception {
        int threadCount = 8;
        int reportsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < reportsPerThread; i++) {
                    // Half use a shared key (will be deduped), half use unique keys
                    if (i % 2 == 0) {
                        collector.report(warning("shared-key", "shared warning", "thread-" + threadId));
                    } else {
                        collector.report(
                                warning("unique-" + threadId + "-" + i, "unique warning " + i, "thread-" + threadId));
                    }
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // "shared-key" should be deduplicated to 1 entry
        // Unique keys: 8 threads * 50 unique = 400
        // Total unique = 401
        int expectedUniqueKeys = 1 + (threadCount * (reportsPerThread / 2));
        assertEquals(expectedUniqueKeys, collector.getProblems().size());

        // shared-key should have count = 8 threads * 50 = 400
        DefaultDiagnosticSummary sharedSummary = collector.getSummary().stream()
                .filter(s -> "shared-key".equals(s.problem().getKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(threadCount * (reportsPerThread / 2), sharedSummary.count());
    }
}
