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
package org.apache.maven.slf4j;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.jline.MessageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class MavenSimpleLoggerTest {

    boolean colorEnabled;

    @BeforeEach
    void setup() {
        colorEnabled = MessageUtils.isColorEnabled();
        MessageUtils.setColorEnabled(false);
    }

    @AfterEach
    void tearDown() {
        MessageUtils.setColorEnabled(colorEnabled);
    }

    @Test
    void includesCauseAndSuppressedExceptionsWhenWritingThrowables(TestInfo testInfo) {
        Exception causeOfSuppressed = new NoSuchElementException("cause of suppressed");
        Exception suppressed = new IllegalStateException("suppressed", causeOfSuppressed);
        suppressed.addSuppressed(new IllegalArgumentException(
                "suppressed suppressed", new ArrayIndexOutOfBoundsException("suppressed suppressed cause")));
        Exception cause = new IllegalArgumentException("cause");
        cause.addSuppressed(suppressed);
        Exception throwable = new RuntimeException("top-level", cause);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new MavenSimpleLogger("logger").writeThrowable(throwable, new PrintStream(output));

        String actual = output.toString(UTF_8);
        List<String> actualLines = Arrays.asList(actual.split(System.lineSeparator()));

        Class<?> testClass = testInfo.getTestClass().get();
        String testMethodName = testInfo.getTestMethod().get().getName();
        String testClassStackTraceLinePattern = "at " + testClass.getName() + "." + testMethodName + "\\("
                + testClass.getSimpleName() + ".java:\\d+\\)";
        List<String> expectedLines = Arrays.asList(
                "java.lang.RuntimeException: top-level",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "Caused by: java.lang.IllegalArgumentException: cause",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Suppressed: java.lang.IllegalStateException: suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Suppressed: java.lang.IllegalArgumentException: suppressed suppressed",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Caused by: java.lang.ArrayIndexOutOfBoundsException: suppressed suppressed cause",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Caused by: java.util.NoSuchElementException: cause of suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>");

        assertLinesMatch(expectedLines, actualLines);
    }
}
