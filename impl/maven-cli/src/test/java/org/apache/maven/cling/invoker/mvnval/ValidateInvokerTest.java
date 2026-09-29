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
import java.util.Set;

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
        // Built once through the seam: it is the expensive part of every test here.
        Session shared = ApiRunner.createSession();
        invoker = new ValidateInvoker(ProtoLookup.builder().build(), null) {
            @Override
            protected Session createSession() {
                // The transports and the settings-derived repository list production adds are
                // not here: this session has neither.
                return shared;
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
        return run(poms, format, Optional.of("raw"));
    }

    /** Defaults to raw: the tests using this helper read files only. */
    private int run(List<String> poms, Optional<String> format, Optional<String> mode) throws Exception {
        ValidateOptions options = mock(ValidateOptions.class);
        when(options.format()).thenReturn(format);
        when(options.mode()).thenReturn(mode);
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

        @Test
        @DisplayName("should read a POM named twice only once")
        void shouldReadAPomNamedTwiceOnce() throws Exception {
            Path pom = writePom("twice", "");

            int exitCode = run(List.of(pom.toString(), "./twice/pom.xml"));

            assertEquals(ValidateInvoker.ERROR, exitCode, output.toString());
            assertEquals(
                    1,
                    output.stream().filter(line -> line.endsWith("pom.xml:")).count(),
                    "one file named two ways is one report: " + output);
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

        @Test
        @DisplayName("should reject an unknown mode with the bad-usage code")
        void shouldRejectUnknownModeWithBadOperation() throws Exception {
            int exitCode = run(List.of(writePom("bad-mode", "").toString()), Optional.empty(), Optional.of("deep"));

            assertEquals(ValidateInvoker.BAD_OPERATION, exitCode);
        }
    }

    @Nested
    @DisplayName("Effective mode")
    class EffectiveTests {

        private Path project(String parent, String relativePath) throws Exception {
            Path root = Files.createDirectories(tempDir.resolve("proj"));
            Files.writeString(root.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>org.test</groupId><artifactId>par</artifactId>
                      <version>1.0</version><packaging>pom</packaging>
                    </project>""");
            Path child = Files.createDirectories(root.resolve("child")).resolve("pom.xml");
            Files.writeString(child, """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.test</groupId><artifactId>%s</artifactId>
                        <version>1.0</version>%s
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                        <dependency><groupId>junit</groupId><artifactId>junit</artifactId></dependency>
                      </dependencies>
                    </project>""".formatted(parent, relativePath));
            return child;
        }

        private int runEffective(Path pom) throws Exception {
            ValidateOptions options = mock(ValidateOptions.class);
            when(options.mode()).thenReturn(Optional.of("effective"));
            when(options.poms()).thenReturn(Optional.of(List.of(pom.toString())));
            ValidateContext context = TestUtils.createMockContext(tempDir, options);
            context.writer = output::add;
            return invoker.execute(context);
        }

        @Test
        @DisplayName("should report a problem that only the effective model shows")
        void shouldReportInheritedProblem() throws Exception {
            // A dependency with no version of its own is checked in validateEffectiveModel, which
            // raw mode never reaches. The parent is next door, so nothing has to be resolved.
            int exitCode = runEffective(project("par", ""));

            assertEquals(ValidateInvoker.ERROR, exitCode, output.toString());
            assertTrue(
                    output.stream().anyMatch(line -> line.contains("'dependencies.dependency.version'")),
                    "the inherited missing version is what effective mode is for: " + output);
        }

        @Test
        @DisplayName("should go looking for a parent that is not on disk")
        void shouldResolveParentThatIsNotOnDisk() throws Exception {
            // Names a parent no file provides, so raw mode is clean and effective mode has to
            // reach for it. The shared session registers no transport, so the reach fails wherever
            // it is run, and the failure is the evidence.
            int exitCode = runEffective(project("absent", "<relativePath/>"));

            assertEquals(ValidateInvoker.ERROR, exitCode, output.toString());
            assertTrue(
                    output.stream().anyMatch(line -> line.contains("Non-resolvable parent POM")),
                    "effective mode has to reach for a parent it cannot read from disk: " + output);
        }

        @Test
        @DisplayName("should refuse --offline rather than reach the network behind it")
        void shouldRefuseOfflineInEffectiveMode() throws Exception {
            ValidateOptions options = mock(ValidateOptions.class);
            when(options.mode()).thenReturn(Optional.of("effective"));
            when(options.offline()).thenReturn(Optional.of(true));
            when(options.poms())
                    .thenReturn(Optional.of(
                            List.of(project("absent", "<relativePath/>").toString())));
            ValidateContext context = TestUtils.createMockContext(tempDir, options);
            context.writer = output::add;

            assertEquals(ValidateInvoker.BAD_OPERATION, invoker.execute(context));
        }

        @Test
        @DisplayName("should see a subproject that is not on disk, as the build does")
        void shouldReportMissingSubproject() throws Exception {
            // The regression this mode was changed for. build() reports nothing about the reactor
            // around a POM, so on its own it called this project clean while mvn refuses to read
            // it. Effective mode therefore runs the raw pass first and keeps its problems.
            Path root = Files.createDirectories(tempDir.resolve("ghosted"));
            Files.writeString(root.resolve("pom.xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>org.test</groupId><artifactId>ghosted</artifactId>
                      <version>1.0</version><packaging>pom</packaging>
                      <modules><module>ghost</module></modules>
                    </project>""");

            int exitCode = runEffective(root.resolve("pom.xml"));

            assertEquals(ValidateInvoker.ERROR, exitCode, output.toString());
            assertTrue(
                    output.stream().anyMatch(line -> line.contains("Child subproject ghost")),
                    "the default mode must not pass a project the build cannot read: " + output);
        }

        @Test
        @DisplayName("should refuse an alternate settings file it cannot honour")
        void shouldRefuseAlternateSettings() throws Exception {
            // Resolution runs on the session ApiRunner builds, which reads ~/.m2/settings.xml and
            // nothing else. Accepting the option would resolve through the default file's mirrors
            // and credentials while the caller believed they had redirected it.
            ValidateOptions options = mock(ValidateOptions.class);
            when(options.mode()).thenReturn(Optional.of("effective"));
            when(options.altUserSettings()).thenReturn(Optional.of("ci-settings.xml"));
            when(options.poms())
                    .thenReturn(Optional.of(List.of(project("par", "").toString())));
            ValidateContext context = TestUtils.createMockContext(tempDir, options);
            context.writer = output::add;

            assertEquals(ValidateInvoker.BAD_OPERATION, invoker.execute(context));
        }

        @Test
        @DisplayName("should accept an alternate settings file in raw mode, where it changes nothing")
        void shouldAcceptAlternateSettingsInRawMode() throws Exception {
            ValidateOptions options = mock(ValidateOptions.class);
            when(options.mode()).thenReturn(Optional.of("raw"));
            when(options.altUserSettings()).thenReturn(Optional.of("ci-settings.xml"));
            when(options.poms())
                    .thenReturn(Optional.of(List.of(project("par", "").toString())));
            ValidateContext context = TestUtils.createMockContext(tempDir, options);
            context.writer = output::add;

            assertEquals(ValidateInvoker.OK, invoker.execute(context), output.toString());
        }

        @Test
        @DisplayName("should give the same verdict whichever order the POMs are named in")
        void shouldNotDependOnArgumentOrder() throws Exception {
            // The regression a shared ModelBuilderSession caused: derived sessions share
            // mappedSources, so a decoy declaring the same groupId:artifactId as a real parent
            // answered the child's parent lookup and changed its verdict. Two roots with the same
            // coordinates and different packaging is the shape that broke.
            Path child = project("par", "");
            Path decoy = Files.createDirectories(tempDir.resolve("decoy")).resolve("pom.xml");
            Files.writeString(decoy, """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>org.test</groupId><artifactId>par</artifactId>
                      <version>1.0</version><packaging>jar</packaging>
                    </project>""");

            List<String> childFirst = runEffectiveOn(List.of(child.toString(), decoy.toString()));
            List<String> decoyFirst = runEffectiveOn(List.of(decoy.toString(), child.toString()));

            assertEquals(
                    Set.copyOf(childFirst),
                    Set.copyOf(decoyFirst),
                    "the same files must get the same problems whichever order they are given in");
        }

        private List<String> runEffectiveOn(List<String> poms) throws Exception {
            ValidateOptions options = mock(ValidateOptions.class);
            when(options.mode()).thenReturn(Optional.of("effective"));
            when(options.poms()).thenReturn(Optional.of(poms));
            ValidateContext context = TestUtils.createMockContext(tempDir, options);
            List<String> lines = new ArrayList<>();
            context.writer = lines::add;
            invoker.execute(context);
            return lines;
        }

        @Test
        @DisplayName("should leave the same project alone in raw mode")
        void shouldNotResolveInRawMode() throws Exception {
            Path pom = project("absent", "<relativePath/>");
            ValidateOptions options = mock(ValidateOptions.class);
            when(options.mode()).thenReturn(Optional.of("raw"));
            when(options.poms()).thenReturn(Optional.of(List.of(pom.toString())));
            ValidateContext context = TestUtils.createMockContext(tempDir, options);
            context.writer = output::add;

            int exitCode = invoker.execute(context);

            assertEquals(ValidateInvoker.OK, exitCode, output.toString());
            assertTrue(
                    output.stream().noneMatch(line -> line.contains("Non-resolvable")),
                    "raw mode must not go looking for the parent: " + output);
        }
    }
}
