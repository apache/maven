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
package org.apache.maven.impl.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ModelBuilder.ModelBuilderSession#validate}, which reads and validates a POM without
 * building it: it returns the problems instead of throwing on them, and it reads only files.
 */
class RawModelReadingTest {

    private static final String DUPLICATE_DEPENDENCY = """
            <dependencies>
              <dependency>
                <groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version>
              </dependency>
              <dependency>
                <groupId>junit</groupId><artifactId>junit</artifactId><version>4.12</version>
              </dependency>
            </dependencies>""";

    private static final String PARENT_AT_LATEST = """
            <parent>
              <groupId>org.test</groupId><artifactId>par</artifactId><version>LATEST</version>
            </parent>""";

    private Session session;
    private ModelBuilder builder;

    @BeforeEach
    void setUp() {
        session = ApiRunner.createSession();
        builder = session.getService(ModelBuilder.class);
    }

    @Test
    void shouldReturnValidationErrorsRatherThanThrowOnThem(@TempDir Path directory) throws IOException {
        Path pom = writePom(directory, "duplicate", coordinates("duplicate") + DUPLICATE_DEPENDENCY);

        assertTrue(
                messages(readRaw(pom), Severity.ERROR).stream().anyMatch(m -> m.contains("must be unique")),
                "the duplicate dependency should come back as a problem, not as an exception");
    }

    @Test
    void shouldReturnWarningThatComesWithNoError(@TempDir Path directory) throws IOException {
        Path pom = writePom(directory, "warning", PARENT_AT_LATEST + coordinates("warning"));

        List<ModelProblem> problems = readRaw(pom);

        assertTrue(
                messages(problems, Severity.WARNING).stream().anyMatch(m -> m.contains("LATEST")),
                "a warning is worth reporting even though nothing was rejected: " + messages(problems));
        assertEquals(List.of(), messages(problems, Severity.ERROR), "nothing should be rejected here");
    }

    @Test
    void shouldReportSubprojectThatIsDeclaredButNotThere(@TempDir Path directory) throws IOException {
        // Found while mapping the reactor, so it is only seen because the whole project is read.
        Path root = writeReactor(directory, "absent");

        assertTrue(
                messages(readRaw(root), Severity.ERROR).stream().anyMatch(m -> m.contains("absent")),
                "a root POM that names a subproject which is not there is worth reporting");
    }

    @Test
    void shouldInferParentVersionOmittedBySubproject(@TempDir Path directory) throws IOException {
        // Leaving out the parent version is what model 4.1.0 is for. It is taken from the parent's
        // own file model, which means the reactor has to be mapped before the subproject is read.
        Path root = writeReactor(directory, "good");
        Path subproject = writeSubproject(root, "good");

        assertEquals(List.of(), messages(readRaw(subproject)), "a versionless parent is not an error in 4.1.0");
    }

    @Test
    void shouldNotReportSiblingThatCannotBeRead(@TempDir Path directory) throws IOException {
        Path root = writeReactor(directory, "good", "broken");
        Path subproject = writeSubproject(root, "good");
        Files.writeString(Files.createDirectories(root.resolveSibling("broken")).resolve("pom.xml"), "<project><bad");

        assertEquals(
                List.of(),
                messages(readRaw(subproject)),
                "the reactor is mapped to find the parent, not to pass judgement on the siblings");
    }

    @Test
    void shouldThrowWhenTheModelCannotBeReadAtAll(@TempDir Path directory) throws IOException {
        Path pom = directory.resolve("pom.xml");
        Files.writeString(pom, "<project><not even xml");

        assertThrows(ModelBuilderException.class, () -> readRaw(pom));
    }

    @Test
    void shouldRejectValidateWhenAnImplementationDoesNotSupportIt() {
        ModelBuilder.ModelBuilderSession unsupporting = request -> {
            throw new UnsupportedOperationException("build is not under test");
        };

        assertThrows(UnsupportedOperationException.class, () -> unsupporting.validate(null));
    }

    @Test
    void shouldValidateSeveralProjectsOnOneSession(@TempDir Path directory) throws IOException {
        // Two reactors meeting in one session under the same coordinates is the case that would
        // break if the session kept a single source per groupId:artifactId.
        Path first = writeSubproject(writeReactor(directory.resolve("first"), "good"), "good");
        Path second = writeSubproject(writeReactor(directory.resolve("second"), "good"), "good");
        ModelBuilder.ModelBuilderSession shared = builder.newSession();

        assertEquals(List.of(), messages(readRaw(shared, first)), "the first project is clean on its own");
        assertEquals(List.of(), messages(readRaw(shared, second)), "the second project sees the same session");
    }

    private List<ModelProblem> readRaw(Path pom) {
        return readRaw(builder.newSession(), pom);
    }

    private List<ModelProblem> readRaw(ModelBuilder.ModelBuilderSession builderSession, Path pom) {
        return builderSession
                .validate(ModelBuilderRequest.builder()
                        .session(session)
                        .source(Sources.buildSource(pom))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .build())
                .getProblemCollector()
                .problems()
                .toList();
    }

    private static List<String> messages(List<ModelProblem> problems) {
        return problems.stream().map(ModelProblem::getMessage).toList();
    }

    private static List<String> messages(List<ModelProblem> problems, Severity severity) {
        return problems.stream()
                .filter(problem -> problem.getSeverity() == severity)
                .map(ModelProblem::getMessage)
                .toList();
    }

    private static String coordinates(String artifactId) {
        return "<groupId>org.test</groupId><artifactId>" + artifactId + "</artifactId><version>1.0</version>";
    }

    private static String project(String modelVersion, String body) {
        return """
                <project xmlns="http://maven.apache.org/POM/%s">
                  <modelVersion>%s</modelVersion>
                  %s
                </project>
                """.formatted(modelVersion, modelVersion, body);
    }

    private static Path writePom(Path directory, String name, String body) throws IOException {
        Path pom = Files.createDirectories(directory.resolve(name)).resolve("pom.xml");
        Files.writeString(pom, project(ModelBuilder.MODEL_VERSION_4_0_0, body));
        return pom;
    }

    /** Writes a 4.1.0 root POM, with the {@code .mvn} directory that makes its directory the root. */
    private static Path writeReactor(Path directory, String... subprojects) throws IOException {
        Path root = Files.createDirectories(directory.resolve("reactor"));
        Files.createDirectories(root.resolve(".mvn"));
        String declared = Arrays.stream(subprojects)
                .map(name -> "<subproject>" + name + "</subproject>")
                .collect(Collectors.joining());
        Files.writeString(
                root.resolve("pom.xml"),
                project(
                        ModelBuilder.MODEL_VERSION_4_1_0,
                        coordinates("reactor") + "<packaging>pom</packaging><subprojects>" + declared
                                + "</subprojects>"));
        return root.resolve("pom.xml");
    }

    private static Path writeSubproject(Path root, String name) throws IOException {
        Path pom = Files.createDirectories(root.resolveSibling(name)).resolve("pom.xml");
        Files.writeString(
                pom,
                project(
                        ModelBuilder.MODEL_VERSION_4_1_0,
                        "<parent><groupId>org.test</groupId><artifactId>reactor</artifactId></parent>" + "<artifactId>"
                                + name + "</artifactId>"));
        return pom;
    }
}
