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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link OutputFormat} enum.
 * Tests that names are matched case insensitively and that unknown names are rejected
 * rather than silently falling back to the default.
 */
@DisplayName("OutputFormat")
class OutputFormatTest {

    @Test
    @DisplayName("should match a name regardless of case")
    void shouldMatchNameRegardlessOfCase() {
        assertSame(OutputFormat.JSON, OutputFormat.parse("json"));
        assertSame(OutputFormat.JSON, OutputFormat.parse("JSON"));
        assertSame(OutputFormat.TEXT, OutputFormat.parse("Text"));
    }

    @Test
    @DisplayName("should reject an unknown name instead of falling back")
    void shouldRejectUnknownName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> OutputFormat.parse("xml"));
        assertTrue(e.getMessage().contains("xml"), "the message should name the rejected value: " + e.getMessage());
        assertTrue(
                e.getMessage().contains("text") && e.getMessage().contains("json"),
                "the message should list the accepted values: " + e.getMessage());
    }

    @Test
    @DisplayName("should render something for every format")
    void shouldRenderSomethingForEveryFormat() {
        Report report = Report.of(Path.of("pom.xml"), List.of());

        for (OutputFormat format : OutputFormat.values()) {
            List<String> lines = new ArrayList<>();
            format.report(List.of(report), Path.of("").toAbsolutePath(), lines::add);

            assertFalse(lines.isEmpty(), "every format must produce output: " + format);
        }
    }
}
