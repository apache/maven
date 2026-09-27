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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.mvnval.ValidateOptions;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ValidateInvoker} class.
 * Tests that the verdict depends on the file alone, that a file which cannot be read does not
 * lose the verdict on the others, and that the exit code follows the reports.
 */
@DisplayName("ValidateInvoker")
class ValidateInvokerTest {

    private static final String DUPLICATE_DEPENDENCY = """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              %s
              <groupId>org.test</groupId>
              <artifactId>test</artifactId>
              <version>1.0</version>
              <dependencies>
                <dependency>
                  <groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version>
                </dependency>
                <dependency>
                  <groupId>junit</groupId><artifactId>junit</artifactId><version>4.12</version>
                </dependency>
              </dependencies>
            </project>
            """;

    private static final String UNRESOLVABLE_PARENT = """
            <parent>
                <groupId>org.nowhere</groupId>
                <artifactId>does-not-exist</artifactId>
                <version>999.0.0</version>
              </parent>""";

    @TempDir
    Path tempDir;

    private List<String> output;
    private ValidateInvoker invoker;

    @BeforeEach
    void setUp() {
        output = new ArrayList<>();
        // The session is created through the seam so the test pays for it once.
        Session session = ApiRunner.createSession();
        invoker = new ValidateInvoker(ProtoLookup.builder().build(), null) {
            @Override
            protected Session createSession() {
                return session;
            }
        };
    }

    private Path writePom(String name, String parent) throws Exception {
        Path pom = Files.createDirectories(tempDir.resolve(name)).resolve("pom.xml");
        Files.writeString(pom, String.format(DUPLICATE_DEPENDENCY, parent));
        return pom;
    }

    private int run(List<String> poms) throws Exception {
        return run(poms, Optional.empty());
    }

    private int run(List<String> poms, Optional<String> format) throws Exception {
        ValidateOptions options = mock(ValidateOptions.class);
        when(options.format()).thenReturn(format);
        when(options.poms()).thenReturn(poms.isEmpty() ? Optional.empty() : Optional.of(poms));

        ValidateContext context = TestUtils.createMockContext(tempDir, options);
        context.writer = output::add;
        return invoker.execute(context);
    }

    @Nested
    @DisplayName("Determinism")
    class DeterminismTests {

        @Test
        @DisplayName("should reach the same verdict whether or not the parent can be resolved")
        void shouldReachSameVerdictRegardlessOfParent() throws Exception {
            Path withParent = writePom("with-parent", UNRESOLVABLE_PARENT);
            Path withoutParent = writePom("without-parent", "");

            int exitWith = run(List.of(withParent.toString()));
            List<String> linesWith = List.copyOf(output);
            output.clear();
            int exitWithout = run(List.of(withoutParent.toString()));

            assertEquals(exitWith, exitWithout, "an unresolvable parent must not change the exit code");
            assertTrue(
                    linesWith.stream().anyMatch(l -> l.contains("must be unique")),
                    "the duplicate dependency should be reported even with an unresolvable parent: " + linesWith);
            assertTrue(
                    output.stream().anyMatch(l -> l.contains("must be unique")),
                    "and also without a parent: " + output);
        }
    }

    @Nested
    @DisplayName("Reporting")
    class ReportingTests {

        @Test
        @DisplayName("should not invent a missing parent version for a 4.1.0 subproject")
        void shouldNotInventMissingParentVersionForSubproject() throws Exception {
            Path root = Files.createDirectories(tempDir.resolve("reactor"));
            Files.createDirectories(root.resolve(".mvn"));
            Files.writeString(root.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.1.0">
                      <modelVersion>4.1.0</modelVersion>
                      <groupId>org.test</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0</version>
                      <packaging>pom</packaging>
                      <subprojects>
                        <subproject>child</subproject>
                      </subprojects>
                    </project>
                    """);
            Path child = Files.createDirectories(root.resolve("child"));
            Files.writeString(child.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.1.0">
                      <modelVersion>4.1.0</modelVersion>
                      <parent>
                        <groupId>org.test</groupId>
                        <artifactId>parent</artifactId>
                      </parent>
                      <artifactId>child</artifactId>
                    </project>
                    """);

            int exitCode = run(List.of(child.resolve("pom.xml").toString()));

            assertEquals(
                    ValidateInvoker.OK,
                    exitCode,
                    "leaving out the parent version is what model 4.1.0 is for, not an error: " + output);
        }

        @Test
        @DisplayName("should not blame a POM for a sibling module that cannot be read")
        void shouldNotBlameSiblingModule() throws Exception {
            Path root = Files.createDirectories(tempDir.resolve("siblings"));
            Files.createDirectories(root.resolve(".mvn"));
            Files.writeString(root.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.1.0">
                      <modelVersion>4.1.0</modelVersion>
                      <groupId>org.test</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0</version>
                      <packaging>pom</packaging>
                      <subprojects>
                        <subproject>good</subproject>
                        <subproject>broken</subproject>
                      </subprojects>
                    </project>
                    """);
            Path good = Files.createDirectories(root.resolve("good"));
            Files.writeString(good.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.1.0">
                      <modelVersion>4.1.0</modelVersion>
                      <parent><groupId>org.test</groupId><artifactId>parent</artifactId></parent>
                      <artifactId>good</artifactId>
                    </project>
                    """);
            Path broken = Files.createDirectories(root.resolve("broken"));
            Files.writeString(broken.resolve("pom.xml"), "<project><not even xml");

            int exitCode = run(List.of(good.resolve("pom.xml").toString()));

            assertEquals(
                    ValidateInvoker.OK,
                    exitCode,
                    "the reactor is mapped to infer the parent version, not to judge the siblings: " + output);
            assertTrue(
                    output.stream().noneMatch(l -> l.contains("broken")),
                    "the broken sibling must not be reported against this file: " + output);
        }

        @Test
        @DisplayName("should report a file that does not exist without losing the others")
        void shouldReportMissingFileWithoutLosingOthers() throws Exception {
            Path good = writePom("good", "");

            int exitCode = run(List.of(tempDir.resolve("nope/pom.xml").toString(), good.toString()));

            assertEquals(ValidateInvoker.ERROR, exitCode);
            assertTrue(
                    output.stream().anyMatch(l -> l.contains("does not exist")),
                    "the missing file should be reported: " + output);
            assertTrue(
                    output.stream().anyMatch(l -> l.contains("must be unique")),
                    "the other file should still be validated: " + output);
        }

        @Test
        @DisplayName("should report a warning that comes with no error, and exit with the warning code")
        void shouldReportWarningWithoutError() throws Exception {
            Path pom = Files.createDirectories(tempDir.resolve("warn-only")).resolve("pom.xml");
            Files.writeString(pom, """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.test</groupId><artifactId>par</artifactId><version>LATEST</version>
                      </parent>
                      <artifactId>child</artifactId>
                    </project>
                    """);

            int exitCode = run(List.of(pom.toString()));

            assertEquals(
                    ValidateInvoker.WARNINGS,
                    exitCode,
                    "warnings without errors are what a gate passes back to maintainers: " + output);
            assertTrue(
                    output.stream().anyMatch(l -> l.contains("WARNING")),
                    "the warning must be reported, not swallowed: " + output);
        }

        @Test
        @DisplayName("should exit with an error when a problem was reported")
        void shouldExitWithErrorWhenProblemReported() throws Exception {
            assertEquals(ValidateInvoker.ERROR, run(List.of(writePom("bad", "").toString())));
        }
    }

    @Nested
    @DisplayName("Extensions")
    class ExtensionTests {

        @Test
        @DisplayName("should stand up no container, so the POM's directory cannot make it load code")
        void shouldStandUpNoContainer() throws Exception {
            // Booting it would resolve and run whatever .mvn/extensions.xml declares, and whatever
            // maven.ext.class.path names in .mvn/maven-user.properties.
            ValidateContext context = TestUtils.createMockContext(tempDir, mock(ValidateOptions.class));

            invoker.container(context);
            invoker.postContainer(context);
            invoker.lookup(context);
            invoker.init(context);
            invoker.settings(context);

            assertNull(context.containerCapsule, "no container");
            assertNull(context.lookup, "nothing to look up from");
            assertNull(context.eventSpyDispatcher, "no event spies");
            assertNull(context.effectiveSettings, "no settings, so no mirrors, proxies or credentials");
        }
    }

    @Nested
    @DisplayName("Format")
    class FormatTests {

        @Test
        @DisplayName("should emit one JSON document when json is asked for")
        void shouldEmitJsonDocument() throws Exception {
            Path pom = writePom("as-json", "");

            int exitCode = run(List.of(pom.toString()), Optional.of("json"));

            assertEquals(ValidateInvoker.ERROR, exitCode);
            assertEquals(1, output.size(), "the whole report is one document, not a line per problem: " + output);
            assertTrue(output.get(0).startsWith("[{"), "expected a JSON array: " + output);
            assertTrue(output.get(0).contains("\"severity\":\"ERROR\""), output.get(0));
        }

        @Test
        @DisplayName("should turn an unparseable command line into the bad-usage code")
        void shouldTurnParseFailureIntoBadOperation() {
            ValidateContext context = TestUtils.createMockContext(tempDir, mock(ValidateOptions.class));
            when(context.invokerRequest.parsingFailed()).thenReturn(true);

            InvokerException.ExitException e =
                    assertThrows(InvokerException.ExitException.class, () -> invoker.validate(context));

            assertEquals(
                    ValidateInvoker.BAD_OPERATION,
                    e.getExitCode(),
                    "the base exits 1, which this tool uses for a rejected POM");
        }

        @Test
        @DisplayName("should reject an unknown format with the bad-usage code, not the parse-failure one")
        void shouldRejectUnknownFormatWithBadOperation() throws Exception {
            // Rejecting this while parsing would mark the invocation unparseable and exit 1,
            // which is not the code the tool documents for bad usage.
            int exitCode = run(List.of(writePom("xml-format", "").toString()), Optional.of("xml"));

            assertEquals(ValidateInvoker.BAD_OPERATION, exitCode);
        }

        @Test
        @DisplayName("should reject a format that only became unknown after interpolation")
        void shouldRejectFormatUnknownAfterInterpolation() throws Exception {
            // The parser checks --format, but options are interpolated afterwards, so a value the
            // parser accepted can still be anything by the time the invoker reads it.
            int exitCode = run(List.of(writePom("bad-format", "").toString()), Optional.of("yaml"));

            assertEquals(ValidateInvoker.BAD_OPERATION, exitCode, "a bad format is a usage error, not a verdict");
        }
    }
}
