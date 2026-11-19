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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JvmConfigParserTest {
    @TempDir
    Path tempDir;

    static Stream<Arguments> arguments() {
        return Stream.of(
                // Empty quoted strings produce empty argument tokens
                Arguments.of("first \"\" third", "\"first\" \"\" \"third\""),
                Arguments.of("'' \"\"", "\"\" \"\""),
                // Tab-separated tokens (whitespace generalisation)
                Arguments.of("first\t\"\"\tthird", "\"first\" \"\" \"third\""),
                // Adjacent quoted fragments merge into one token
                Arguments.of("fi\"\"rst ''third", "\"first\" \"third\""),
                // Space inside double quotes is preserved
                Arguments.of("-Dname=\"hello world\"", "\"-Dname=hello world\""),
                // Windows paths inside double quotes: backslash is literal
                Arguments.of(
                        "\"-javaagent:D:\\a\\maven\\target\\agent.jar\"",
                        "\"-javaagent:D:\\\\a\\\\maven\\\\target\\\\agent.jar\""),
                // Single-quoted Windows path: backslash is literal
                Arguments.of("'-Dpath=C:\\temp\\config'", "\"-Dpath=C:\\\\temp\\\\config\""),
                // Blank / whitespace-only line produces empty output
                Arguments.of("  \t  ", ""));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void tokenizesArguments(String input, String expected) throws Exception {
        Path config = tempDir.resolve("jvm.config");
        Files.writeString(config, input);
        assertEquals(expected, JvmConfigParser.parseJvmConfig(config, tempDir.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\" '", "'' \""})
    void rejectsUnclosedQuotes(String input) throws Exception {
        Path config = tempDir.resolve("jvm.config");
        Files.writeString(config, input);
        assertThrows(IllegalArgumentException.class, () -> JvmConfigParser.parseJvmConfig(config, tempDir.toString()));
    }

    @Test
    void preservesProjectBasedirAfterTokenization() throws Exception {
        Path config = tempDir.resolve("jvm.config");
        Files.writeString(config, "'-Dfirst=${MAVEN_PROJECTBASEDIR}/first' \"-Dsecond=$MAVEN_PROJECTBASEDIR/second\"");
        assertEquals(
                "\"-Dfirst=/tmp/maven/first\" \"-Dsecond=/tmp/maven/second\"",
                JvmConfigParser.parseJvmConfig(config, "/tmp/maven"));
    }

    @Test
    void preservesWindowsProjectBasedirBackslashes() throws Exception {
        Path config = tempDir.resolve("jvm.config");
        Files.writeString(config, "-Dfirst=${MAVEN_PROJECTBASEDIR}/first");
        // Windows backslashes in the substituted path must be preserved as literal characters
        assertEquals("\"-Dfirst=D:\\\\a\\\\maven/first\"", JvmConfigParser.parseJvmConfig(config, "D:\\a\\maven"));
    }
}
