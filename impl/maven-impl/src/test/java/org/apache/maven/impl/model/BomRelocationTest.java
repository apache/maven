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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.standalone.ApiRunner;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BomRelocationTest {
    @TempDir
    Path tempDir;

    @Test
    void importsRelocatedBom() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        Model model = build(poms, management(bom("old"))).getEffectiveModel();
        assertEquals(List.of("library:2"), managedDependencies(model));
    }

    @Test
    void followsPartialRelocationChain() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<groupId>relocated</groupId>")),
                "relocated:old:1", pom("relocated", "old", "1", relocation("<artifactId>new</artifactId>")),
                "relocated:new:1", pom("relocated", "new", "1", relocation("<version>2</version>")),
                "relocated:new:2", pom("relocated", "new", "2", management(dependency("library", "2"))));
        assertEquals(
                List.of("library:2"),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void ignoresManagementFromRelocationSourceAndPreservesPrecedence() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                        pom(
                                "test",
                                "old",
                                "1",
                                relocation("<artifactId>new</artifactId>") + management(dependency("obsolete", "1"))),
                "test:new:1",
                        pom("test", "new", "1", management(dependency("library", "2") + dependency("other", "2"))),
                "test:ordinary:1", pom("test", "ordinary", "1", management(dependency("other", "3"))));
        String content = management(dependency("library", "4") + bom("old") + bom("ordinary"));
        assertEquals(
                List.of("library:4", "other:2"),
                managedDependencies(build(poms, content).getEffectiveModel()));
    }

    @Test
    void preservesManagementForUnchangedRelocation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                pom(
                        "test",
                        "old",
                        "1",
                        relocation("<artifactId>old</artifactId>") + management(dependency("library", "2"))));
        assertEquals(
                List.of("library:2"),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void preservesManagementForMessageOnlyRelocation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                pom(
                        "test",
                        "old",
                        "1",
                        relocation("<message>Use the current release</message>")
                                + management(dependency("library", "2"))));
        assertEquals(
                List.of("library:2"),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void detectsRelocationCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                        "test:new:1", pom("test", "new", "1", relocation("<artifactId>old</artifactId>"))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void detectsRelocationThenImportCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                        "test:new:1", pom("test", "new", "1", management(bom("old")))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void detectsImportThenRelocationCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", management(bom("new"))),
                        "test:new:1", pom("test", "new", "1", relocation("<artifactId>old</artifactId>"))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void reportsUnresolvableRelocationTarget() {
        Map<String, String> poms =
                Map.of("test:old:1", pom("test", "old", "1", relocation("<artifactId>missing</artifactId>")));
        ModelBuilderException exception =
                assertThrows(ModelBuilderException.class, () -> build(poms, management(bom("old"))));
        assertTrue(exception.getMessage().contains("Non-resolvable import POM"), exception.getMessage());
        assertTrue(exception.getMessage().contains("test:missing"), exception.getMessage());
    }

    @Test
    void partialRelocationKeepsVersionSelectedFromRange() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:2", pom("test", "old", "2", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "1"))),
                "test:new:2", pom("test", "new", "2", management(dependency("library", "2"))),
                "test:new:3", pom("test", "new", "3", management(dependency("library", "3"))));
        String content = management("<dependency><groupId>test</groupId><artifactId>old</artifactId>"
                + "<version>[1,4)</version><type>pom</type><scope>import</scope></dependency>");
        assertEquals(
                List.of("library:2"), managedDependencies(build(poms, content).getEffectiveModel()));
    }

    @Test
    void doesNotSubstituteAnotherTargetVersionAfterResolvingRange() {
        Map<String, String> poms = Map.of(
                "test:old:2", pom("test", "old", "2", relocation("<artifactId>new</artifactId>")),
                "test:new:3", pom("test", "new", "3", management(dependency("library", "3"))));
        String content = management("<dependency><groupId>test</groupId><artifactId>old</artifactId>"
                + "<version>[1,4)</version><type>pom</type><scope>import</scope></dependency>");
        ModelBuilderException exception = assertThrows(ModelBuilderException.class, () -> build(poms, content));
        assertTrue(exception.getMessage().contains("Non-resolvable import POM"), exception.getMessage());
        assertTrue(exception.getMessage().contains("test:new:2"), exception.getMessage());
    }

    @Test
    void importsRelocatedReactorBom() throws Exception {
        Files.createDirectory(tempDir.resolve(".mvn"));
        for (String artifact : List.of("old", "new", "consumer")) {
            Path directory = Files.createDirectory(tempDir.resolve(artifact));
            String content =
                    switch (artifact) {
                        case "old" -> relocation("<artifactId>new</artifactId>");
                        case "new" -> management(dependency("library", "2"));
                        default -> management(bom("old"));
                    };
            Files.writeString(directory.resolve("pom.xml"), pom("test", artifact, "1", content));
        }
        Path root = tempDir.resolve("pom.xml");
        Files.writeString(
                root,
                pom(
                        "test",
                        "root",
                        "1",
                        "<modules><module>old</module><module>new</module><module>consumer</module></modules>"));
        Session session = ApiRunner.createSession();
        ModelBuilderResult result = session.getService(ModelBuilder.class)
                .newSession()
                .build(ModelBuilderRequest.builder()
                        .session(session)
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .recursive(true)
                        .source(Sources.buildSource(root))
                        .build());
        Model consumer = result.getChildren().stream()
                .map(ModelBuilderResult::getEffectiveModel)
                .filter(model -> "consumer".equals(model.getArtifactId()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("library:2"), managedDependencies(consumer));
    }

    @Test
    void ordinaryMissingImportHasSameBehaviorThroughRelocation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(bom("missing"))));
        assertEquals(
                List.of(),
                managedDependencies(build(poms, management(bom("new"))).getEffectiveModel()));
        assertEquals(
                List.of(),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void ordinaryImportCycleHasSameBehaviorThroughRelocation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(bom("nested"))),
                "test:nested:1", pom("test", "nested", "1", management(bom("new"))));
        assertEquals(
                List.of(),
                managedDependencies(build(poms, management(bom("new"))).getEffectiveModel()));
        assertEquals(
                List.of(),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void skipsOrdinaryBomWithUnresolvedSystemPath() throws Exception {
        Map<String, String> poms = Map.of("test:new:1", pom("test", "new", "1", unresolvedSystemManagement()));
        assertEquals(
                List.of(),
                managedDependencies(build(poms, management(bom("new"))).getEffectiveModel()));
    }

    @Test
    void skipsRelocatedBomWithUnresolvedSystemPath() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", unresolvedSystemManagement()));
        assertEquals(
                List.of(),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void successfulRelocationDoesNotChangeLaterOrdinaryImportValidation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))),
                "test:ordinary:1", pom("test", "ordinary", "1", unresolvedSystemManagement()));
        assertEquals(
                List.of("library:2"),
                managedDependencies(
                        build(poms, management(bom("old") + bom("ordinary"))).getEffectiveModel()));
    }

    private static String unresolvedSystemManagement() {
        return management("<dependency><groupId>test</groupId><artifactId>tool</artifactId>"
                + "<version>1</version><scope>system</scope>"
                + "<systemPath>${test.dir}/${test.file}</systemPath></dependency>");
    }

    @Test
    void relocatedRepositoryBomStillRejectsSystemScope() throws Exception {
        Path systemPath = Files.createFile(tempDir.resolve("tool.jar"));
        String systemDependency = "<dependency><groupId>test</groupId><artifactId>tool</artifactId>"
                + "<version>1</version><scope>system</scope><systemPath>" + systemPath
                + "</systemPath></dependency>";
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2") + systemDependency)));
        assertEquals(
                List.of("library:2"),
                managedDependencies(build(poms, management(bom("old"))).getEffectiveModel()));
    }

    @Test
    void exclusionsOnRelocatedImportDoNotPolluteCachedTarget() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1",
                        pom("test", "new", "1", management(dependency("library", "2") + dependency("excluded", "3"))));
        String exclusions = "<exclusions><exclusion><groupId>test</groupId><artifactId>excluded</artifactId>"
                + "</exclusion></exclusions>";
        Model model =
                build(poms, management(bom("old", exclusions) + bom("new"))).getEffectiveModel();
        assertEquals(List.of("library:2", "excluded:3"), managedDependencies(model));
        assertEquals(
                "excluded",
                model.getDependencyManagement()
                        .getDependencies()
                        .get(0)
                        .getExclusions()
                        .get(0)
                        .getArtifactId());
        assertTrue(model.getDependencyManagement()
                .getDependencies()
                .get(1)
                .getExclusions()
                .isEmpty());
    }

    @Test
    void importsRelocatedBomTypeWithoutScope() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        String content = management("<dependency><groupId>test</groupId><artifactId>old</artifactId>"
                + "<version>1</version><type>bom</type></dependency>");
        Model model = build(poms, content).getEffectiveModel();
        assertEquals(List.of("library:2"), managedDependencies(model));
    }

    @Test
    void reportsRelocationCoordinatesAndMessage() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                        pom(
                                "test",
                                "old",
                                "1",
                                relocation("<artifactId>new</artifactId><message>Please update the import</message>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        ModelBuilderResult result = build(poms, management(bom("old")));
        assertTrue(
                result.getProblemCollector()
                        .problems()
                        .anyMatch(
                                p -> p.getMessage()
                                        .equals(
                                                "The import POM test:old:1 has been relocated to test:new:1: Please update the import")));
    }

    @Test
    void rejectsInvalidRelocationCoordinates() {
        for (String component : List.of("groupId", "artifactId", "version")) {
            for (String value : List.of("..", "bad/name", "bad\\name", "bad:name", "bad&#9;name")) {
                Map<String, String> poms = Map.of(
                        "test:old:1",
                        pom("test", "old", "1", relocation("<" + component + ">" + value + "</" + component + ">")));
                ModelBuilderException exception =
                        assertThrows(ModelBuilderException.class, () -> build(poms, management(bom("old"))));
                assertTrue(exception.getMessage().contains("Invalid relocation " + component), exception.getMessage());
            }
        }
    }

    @Test
    void repeatedImportsReuseRelocatedManagement() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:alias:1", pom("test", "alias", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        assertEquals(
                List.of("library:2"),
                managedDependencies(build(poms, management(bom("old") + bom("alias") + bom("new")))
                        .getEffectiveModel()));
    }

    private void assertCycle(Map<String, String> poms, String cycle) {
        ModelBuilderException exception =
                assertThrows(ModelBuilderException.class, () -> build(poms, management(bom("old"))));
        assertTrue(exception.getMessage().contains("form a cycle: " + cycle), exception.getMessage());
    }

    private ModelBuilderResult build(Map<String, String> poms, String content) throws Exception {
        Path projectDir = Files.createTempDirectory(tempDir, "project-");
        Files.createDirectory(projectDir.resolve(".mvn"));
        Path remoteRepo = projectDir.resolve("remote-repo");
        Map<Path, List<String>> versions = new HashMap<>();
        for (Map.Entry<String, String> entry : poms.entrySet()) {
            String[] coordinates = entry.getKey().split(":");
            Path path = remoteRepo.resolve(coordinates[0].replace('.', '/') + "/" + coordinates[1] + "/"
                    + coordinates[2] + "/" + coordinates[1] + "-" + coordinates[2] + ".pom");
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue());
            versions.computeIfAbsent(path.getParent().getParent(), key -> new ArrayList<>())
                    .add(coordinates[2]);
        }
        for (Map.Entry<Path, List<String>> entry : versions.entrySet()) {
            String metadataVersions = entry.getValue().stream()
                    .map(version -> "<version>" + version + "</version>")
                    .collect(java.util.stream.Collectors.joining());
            Files.writeString(
                    entry.getKey().resolve("maven-metadata.xml"),
                    "<metadata><versioning><versions>" + metadataVersions + "</versions></versioning></metadata>");
        }
        Path consumer = projectDir.resolve("pom.xml");
        Files.writeString(consumer, pom("test", "consumer", "1", content));
        Session session = ApiRunner.createSession(
                injector -> injector.bindInstance(BomRelocationTest.class, this), projectDir.resolve("local-repo"));
        session = session.withRemoteRepositories(List.of(session.createRemoteRepository(
                RemoteRepository.CENTRAL_ID, remoteRepo.toUri().toString())));
        return session.getService(ModelBuilder.class)
                .newSession()
                .build(ModelBuilderRequest.builder()
                        .session(session)
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .source(Sources.buildSource(consumer))
                        .build());
    }

    private List<String> managedDependencies(Model model) {
        return model.getDependencyManagement().getDependencies().stream()
                .map(d -> d.getArtifactId() + ":" + d.getVersion())
                .toList();
    }

    private static String pom(String groupId, String artifactId, String version, String content) {
        return "<project><modelVersion>4.0.0</modelVersion><groupId>" + groupId
                + "</groupId><artifactId>" + artifactId + "</artifactId><version>" + version
                + "</version><packaging>pom</packaging>" + content + "</project>";
    }

    private static String relocation(String coordinates) {
        return "<distributionManagement><relocation>" + coordinates + "</relocation></distributionManagement>";
    }

    private static String management(String dependencies) {
        return "<dependencyManagement><dependencies>" + dependencies + "</dependencies></dependencyManagement>";
    }

    private static String dependency(String artifactId, String version) {
        return "<dependency><groupId>test</groupId><artifactId>" + artifactId + "</artifactId><version>" + version
                + "</version></dependency>";
    }

    private static String bom(String artifactId) {
        return bom(artifactId, "");
    }

    private static String bom(String artifactId, String extra) {
        return "<dependency><groupId>test</groupId><artifactId>" + artifactId
                + "</artifactId><version>1</version><type>pom</type><scope>import</scope>" + extra + "</dependency>";
    }

    @Provides
    @Named(FileTransporterFactory.NAME)
    static FileTransporterFactory newFileTransporterFactory() {
        return new FileTransporterFactory();
    }
}
