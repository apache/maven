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
                Arguments.of("first \"\" third", "\"first\" \"\" \"third\""),
                Arguments.of("'' \"\"", "\"\" \"\""),
                Arguments.of("first\t\"\"\tthird", "\"first\" \"\" \"third\""),
                Arguments.of("fi\"\"rst ''third", "\"first\" \"third\""),
                Arguments.of("-Dname=\"hello world\"", "\"-Dname=hello world\""),
                Arguments.of("hello\\ world", "\"hello\\\\\" \"world\""),
                Arguments.of("\"a\\\"b\"", "\"a\\\"b\""),
                Arguments.of(
                        "-javaagent:D:\\a\\maven\\target\\agent.jar",
                        "\"-javaagent:D:\\\\a\\\\maven\\\\target\\\\agent.jar\""),
                Arguments.of("\"-Dpath=C:\\Program Files\\Maven\"", "\"-Dpath=C:\\\\Program Files\\\\Maven\""),
                Arguments.of(
                        "-javaagent:\\\\server\\share\\agent.jar",
                        "\"-javaagent:\\\\\\\\server\\\\share\\\\agent.jar\""),
                Arguments.of("'-Dpath=C:\\temp\\config'", "\"-Dpath=C:\\\\temp\\\\config\""),
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
                "\"-Dfirst=C:\\\\Program Files\\\\Maven/first\" \"-Dsecond=C:\\\\Program Files\\\\Maven/second\"",
                JvmConfigParser.parseJvmConfig(config, "C:\\Program Files\\Maven"));
    }

    @Test
    void preservesUnquotedProjectBasedirWithSpaces() throws Exception {
        Path config = tempDir.resolve("jvm.config");
        Files.writeString(config, "-Dpath=$MAVEN_PROJECTBASEDIR/settings");
        assertEquals(
                "\"-Dpath=C:\\\\Program Files\\\\Maven/settings\"",
                JvmConfigParser.parseJvmConfig(config, "C:\\Program Files\\Maven"));
    }
}
