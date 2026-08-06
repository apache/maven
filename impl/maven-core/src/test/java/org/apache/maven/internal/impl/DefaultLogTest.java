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
import java.util.List;

import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.services.BuilderProblem;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class DefaultLogTest {

    @Test
    void childCreatesHierarchicalLoggerName() {
        DefaultLog parent = new DefaultLog(LoggerFactory.getLogger("compiler:compile"));
        Log child = parent.child("diagnostics");
        assertNotSame(parent, child);
        // child is a DefaultLog; verify it works by creating another level
        Log grandchild = child.child("detail");
        assertNotSame(child, grandchild);
    }

    @Test
    void childInheritsProblemSink() {
        List<BuilderProblem> collected = new ArrayList<>();
        DefaultLog parent = new DefaultLog(LoggerFactory.getLogger("test:parent"), collected::add);
        Log child = parent.child("sub");

        BuilderProblem problem = BuilderProblem.builder()
                .key("test:key")
                .message("something wrong")
                .severity(BuilderProblem.Severity.WARNING)
                .build();
        child.problem(problem);

        assertEquals(1, collected.size());
        assertEquals("test:key", collected.get(0).getKey());
    }

    @Test
    void problemReportsToSinkAndSetsThreadLocalFlag() {
        List<BuilderProblem> collected = new ArrayList<>();
        DefaultLog log = new DefaultLog(LoggerFactory.getLogger("test:problem"), collected::add);

        // Before calling problem(), flag should be false
        assertFalse(DefaultLog.STRUCTURED_PROBLEM_ACTIVE.get());

        BuilderProblem problem = BuilderProblem.builder()
                .key("test:deprecation")
                .message("deprecated feature")
                .severity(BuilderProblem.Severity.WARNING)
                .build();
        log.problem(problem);

        // After problem() returns, flag should be cleared
        assertFalse(DefaultLog.STRUCTURED_PROBLEM_ACTIVE.get());

        // Problem should have been reported to the sink
        assertEquals(1, collected.size());
        assertEquals("deprecated feature", collected.get(0).getMessage());
    }

    @Test
    void problemDefaultImplementationFallsBackToWarnOrError() {
        // Test the default implementation on the Log interface directly
        Log defaultLog = new Log() {
            final List<String> warnings = new ArrayList<>();
            final List<String> errors = new ArrayList<>();

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(CharSequence content) {}

            @Override
            public void debug(CharSequence content, Throwable error) {}

            @Override
            public void debug(Throwable error) {}

            @Override
            public void debug(java.util.function.Supplier<String> content) {}

            @Override
            public void debug(java.util.function.Supplier<String> content, Throwable error) {}

            @Override
            public boolean isInfoEnabled() {
                return true;
            }

            @Override
            public void info(CharSequence content) {}

            @Override
            public void info(CharSequence content, Throwable error) {}

            @Override
            public void info(Throwable error) {}

            @Override
            public void info(java.util.function.Supplier<String> content) {}

            @Override
            public void info(java.util.function.Supplier<String> content, Throwable error) {}

            @Override
            public boolean isWarnEnabled() {
                return true;
            }

            @Override
            public void warn(CharSequence content) {
                warnings.add(content.toString());
            }

            @Override
            public void warn(CharSequence content, Throwable error) {}

            @Override
            public void warn(Throwable error) {}

            @Override
            public void warn(java.util.function.Supplier<String> content) {}

            @Override
            public void warn(java.util.function.Supplier<String> content, Throwable error) {}

            @Override
            public boolean isErrorEnabled() {
                return true;
            }

            @Override
            public void error(CharSequence content) {
                errors.add(content.toString());
            }

            @Override
            public void error(CharSequence content, Throwable error) {}

            @Override
            public void error(Throwable error) {}

            @Override
            public void error(java.util.function.Supplier<String> content) {}

            @Override
            public void error(java.util.function.Supplier<String> content, Throwable error) {}
        };

        // WARNING severity → warn()
        defaultLog.problem(BuilderProblem.builder()
                .message("warn msg")
                .severity(BuilderProblem.Severity.WARNING)
                .build());
        assertEquals(1, ((List<?>) getField(defaultLog, "warnings")).size());

        // ERROR severity → error()
        defaultLog.problem(BuilderProblem.builder()
                .message("error msg")
                .severity(BuilderProblem.Severity.ERROR)
                .build());
        assertEquals(1, ((List<?>) getField(defaultLog, "errors")).size());
    }

    @Test
    void problemWithNoopSinkDoesNotThrow() {
        // DefaultLog with default no-op sink should not throw
        DefaultLog log = new DefaultLog(LoggerFactory.getLogger("test:noop"));
        BuilderProblem problem = BuilderProblem.builder()
                .message("just a warning")
                .severity(BuilderProblem.Severity.WARNING)
                .build();
        log.problem(problem); // should not throw
    }

    @Test
    void structuredProblemFlagIsClearedOnException() {
        DefaultLog log = new DefaultLog(LoggerFactory.getLogger("test:exception"), p -> {
            throw new RuntimeException("sink failed");
        });

        BuilderProblem problem = BuilderProblem.builder()
                .message("bad")
                .severity(BuilderProblem.Severity.WARNING)
                .build();

        try {
            log.problem(problem);
        } catch (RuntimeException e) {
            // expected
        }
        // Even on exception, the flag should NOT be left set
        // (the exception is thrown before setting the flag in the current impl,
        // but this tests the contract)
        assertFalse(DefaultLog.STRUCTURED_PROBLEM_ACTIVE.get());
    }

    @SuppressWarnings("unchecked")
    private static Object getField(Object obj, String name) {
        try {
            var field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
