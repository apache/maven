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
package org.slf4j.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class MavenSimpleLoggerTest {

    @Test
    public void includesCauseAndSuppressedExceptionsWhenWritingThrowables() throws Exception {
        Exception causeOfSuppressed = new NoSuchElementException("cause of suppressed");
        Exception suppressed = new IllegalStateException("suppressed", causeOfSuppressed);
        suppressed.addSuppressed(new IllegalArgumentException(
                "suppressed suppressed", new ArrayIndexOutOfBoundsException("suppressed suppressed cause")));
        Exception cause = new IllegalArgumentException("cause");
        cause.addSuppressed(suppressed);
        Exception throwable = new RuntimeException("top-level", cause);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new MavenSimpleLogger("logger").writeThrowable(throwable, new PrintStream(output));

        String actual = output.toString(UTF_8.name());

        List<String> expectedLines = Arrays.asList(
                "java.lang.RuntimeException: top-level",
                "Caused by: java.lang.IllegalArgumentException: cause",
                "    Suppressed: java.lang.IllegalStateException: suppressed",
                "        Suppressed: java.lang.IllegalArgumentException: suppressed suppressed",
                "        Caused by: java.lang.ArrayIndexOutOfBoundsException: suppressed suppressed cause",
                "    Caused by: java.util.NoSuchElementException: cause of suppressed");

        assertThat(actual, stringContainsInOrder(expectedLines));
    }
}
