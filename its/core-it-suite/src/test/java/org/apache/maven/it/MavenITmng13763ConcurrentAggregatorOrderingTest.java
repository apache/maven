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
package org.apache.maven.it;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenITmng13763ConcurrentAggregatorOrderingTest extends AbstractMavenIntegrationTestCase {
    private static final String GROUP = "org.apache.maven.its.mng13763";
    private static final String VERSION = "1.0-SNAPSHOT";
    private static final String PROBE = "aggregator-ordering-probe-maven-plugin";

    public MavenITmng13763ConcurrentAggregatorOrderingTest() {
        super("[4.0.0-rc-1,)");
    }

    @Test
    public void testSiblingParentDoesNotReplaceChildAggregator() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("siblings");
        project(root, "", "<artifactId>root</artifactId><modules><module>parent</module><module>child</module></modules>");
        project(root.resolve("parent"), "", "<artifactId>parent</artifactId>" + plugin(VERSION, "", execution("aggregate", "package", "aggregate", "")));
        project(root.resolve("child"), parent("parent", "../parent/pom.xml"), "<artifactId>child</artifactId>");
        build(root, "package").execute();
        assertTrue(Files.exists(root.resolve("child/target/aggregate-observed.txt")));
    }

    @Test
    public void testExecutionInheritanceOverridesPlugin() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("override");
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", "package", "aggregate", "<inherited>true</inherited>")));
        project(root.resolve("a"), parent("root", "../pom.xml"), "<artifactId>a</artifactId>" + plugin(VERSION, "", ""));
        build(root, "package").execute();
        assertTrue(Files.exists(root.resolve("target/aggregate-observed.txt")));
        assertFalse(Files.exists(root.resolve("a/target/aggregate-observed.txt")));
    }

    @Test
    public void testPluginVersionChangesAggregatorFlag() throws Exception {
        verifyPluginVersion(false);
    }

    @Test
    public void testPluginVersionChangesDefaultPhase() throws Exception {
        verifyPluginVersion(true);
    }

    private void verifyPluginVersion(boolean defaultPhase) throws Exception {
        Path dir = extractPlugin();
        Path pom = dir.resolve("plugin/pom.xml");
        Path source = dir.resolve("plugin/src/main/java/org/apache/maven/its/mng13763/AggregateMojo.java");
        String originalPom = Files.readString(pom);
        String originalSource = Files.readString(source);
        Files.writeString(pom, originalPom.replace("<version>1.0-SNAPSHOT</version>", "<version>0.9-SNAPSHOT</version>"));
        Files.writeString(source, defaultPhase
                ? originalSource.replace("LifecyclePhase.PACKAGE", "LifecyclePhase.COMPILE")
                : originalSource.replace("aggregator = true", "aggregator = false"));
        try {
            installPlugin(dir);
        } finally {
            Files.writeString(pom, originalPom);
            Files.writeString(source, originalSource);
        }
        installPlugin(dir);
        Path root = dir.resolve(defaultPhase ? "default-phase" : "versions");
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module></modules>"
                + plugin("0.9-SNAPSHOT", "", execution("aggregate", defaultPhase ? null : "package", "aggregate", "")));
        project(root.resolve("a"), parent("root", "../pom.xml"), "<artifactId>a</artifactId>" + plugin(VERSION, "", ""));
        build(root, "package").execute();
        assertTrue(Files.exists(root.resolve("a/target/aggregate-observed.txt")));
    }

    @Test
    public void testPreparePackageAggregatorWaitsForChildren() throws Exception {
        verifyAlias("prepare-package");
    }

    @Test
    public void testProcessClassesAggregatorWaitsForChildren() throws Exception {
        verifyAlias("process-classes");
    }

    private void verifyAlias(String phase) throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("alias-" + phase);
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", phase, "aggregate", ""))
                        .replace("<executions>", "<configuration><expectedAggregates><expectedAggregate>a/target/touched.txt</expectedAggregate></expectedAggregates></configuration><executions>"));
        project(root.resolve("a"), parent("root", "../pom.xml"), "<artifactId>a</artifactId>"
                + plugin(VERSION, "", execution("touch", phase, "touch", "")));
        Verifier verifier = build(root, "package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertTrue(Files.exists(root.resolve("target/aggregate-observed.txt")));
    }

    @Test
    public void testSamePhaseConsumerRunsAfterAggregator() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("sequence");
        project(root, "", "<artifactId>root</artifactId>" + plugin(VERSION, "", execution("ordered", "package", "aggregate</goal><goal>check", "")));
        Verifier verifier = build(root, "package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testVerifyAggregatorDoesNotRunAfterNestedTestFailure() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("failure");
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module><module>b</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", "verify", "aggregate", "")));
        for (String child : new String[] {"a", "b"}) {
            project(root.resolve(child), parent("root", "../pom.xml"), "<artifactId>" + child + "</artifactId>"
                    + plugin(VERSION, "", execution("touch", "test", "touch", "")));
        }
        Verifier verifier = build(root, "verify");
        verifier.addCliArgument("--fail-at-end");
        verifier.addCliArgument("-DfailModule=a");
        assertThrows(VerificationException.class, verifier::execute);
        assertFalse(Files.exists(root.resolve("target/aggregate-observed.txt")));
        assertTrue(Files.exists(root.resolve("b/target/touched.txt")));
    }

    @Test
    public void testDirectAggregatorGoalKeepsCommandLineSemantics() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("direct");
        project(root, "", "<artifactId>root</artifactId>");
        Verifier verifier = build(root, GROUP + ":" + PROBE + ":" + VERSION + ":aggregate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertTrue(Files.exists(root.resolve("target/aggregate-observed.txt")));
    }

    @Test
    public void testMultipleAggregatorsPreserveInterveningGoals() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("multiple");
        project(root, "", "<artifactId>root</artifactId>" + plugin(VERSION, "",
                execution("prefix", "package", "touch", "")
                        + execution("first", "package", "aggregate", "<configuration><requireOwnTouch>true</requireOwnTouch></configuration>")
                        + execution("middle", "package", "check", "")
                        + execution("second", "package", "aggregate", "<configuration><markerName>second-aggregate.txt</markerName><expectedAggregates><expectedAggregate>target/checked.txt</expectedAggregate></expectedAggregates></configuration>")
                        + execution("suffix", "package", "check", "<configuration><expectedMarker>second-aggregate.txt</expectedMarker></configuration>")));
        for (int threads : new int[] {1, 4}) {
            Verifier verifier = build(root, "package", threads);
            verifier.execute();
            verifier.verifyErrorFreeLog();
        }
    }

    @Test
    public void testEarlierChildAggregatorCompletesBeforeParent() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("nested-alias");
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", "package", "aggregate",
                        "<configuration><expectedAggregates><expectedAggregate>a/target/checked.txt</expectedAggregate></expectedAggregates></configuration>")));
        project(root.resolve("a"), parent("root", "../pom.xml"), "<artifactId>a</artifactId>"
                + plugin(VERSION, "", execution("aggregate", "prepare-package", "aggregate", "")
                        + execution("check", "package", "check", "")));
        Verifier verifier = build(root, "package", 4);
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testCompileAggregatorDoesNotBlockChildReadiness() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("readiness");
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", "compile", "aggregate",
                        "<configuration><expectedAggregates><expectedAggregate>a/target/touched.txt</expectedAggregate></expectedAggregates></configuration>")
                        + execution("check", "package", "check", "")));
        project(root.resolve("a"), parent("root", "../pom.xml"), "<artifactId>a</artifactId>"
                + "<dependencies>" + dependency("root", "pom", "") + "</dependencies>"
                + plugin(VERSION, "", execution("touch", "compile", "touch", "")));
        Verifier verifier = build(root, "package", 4);
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testConsumerWithTransitiveProducerEdgeWaitsForAggregate() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("transitive");
        project(root, "", "<artifactId>root</artifactId><modules><module>group</module><module>consumer</module></modules>");
        project(root.resolve("group"), parent("root", "../pom.xml"), "<artifactId>group</artifactId><modules><module>a</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", "package", "aggregate", "<configuration><attachArtifact>true</attachArtifact></configuration>")));
        project(root.resolve("group/a"), parent("group", "../pom.xml"), "<artifactId>a</artifactId><dependencies>" + dependency("group", "pom", "") + "</dependencies>");
        project(root.resolve("consumer"), parent("root", "../pom.xml"), "<artifactId>consumer</artifactId><dependencies>"
                + dependency("a", "pom", "") + dependency("group", "jar", "<classifier>aggregate</classifier>") + "</dependencies>"
                + plugin(VERSION, "", execution("consume", "compile", "consume", "")));
        Verifier verifier = build(root, "package", 4);
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Aggregate artifact resolved");
    }

    @Test
    public void testFailureInSamePhaseSuffixPreventsLaterAggregation() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("suffix-failure");
        project(root, "", "<artifactId>root</artifactId>" + plugin(VERSION, "",
                execution("package", "package", "aggregate</goal><goal>touch", "")
                        + execution("verify", "verify", "aggregate", "<configuration><markerName>verify-aggregate.txt</markerName></configuration>")));
        Verifier verifier = build(root, "verify");
        verifier.addCliArgument("--fail-at-end");
        verifier.addCliArgument("-DfailModule=suffix-failure");
        assertThrows(VerificationException.class, verifier::execute);
        assertTrue(Files.exists(root.resolve("target/aggregate-observed.txt")));
        assertFalse(Files.exists(root.resolve("target/verify-aggregate.txt")));
    }

    @Test
    public void testMixedLifecycleAndDirectAggregatorGoals() throws Exception {
        Path dir = preparePlugin();
        Path root = dir.resolve("mixed");
        project(root, "", "<artifactId>root</artifactId><modules><module>a</module></modules>"
                + plugin(VERSION, "<inherited>false</inherited>", execution("aggregate", "package", "aggregate</goal><goal>check", "")));
        project(root.resolve("a"), parent("root", "../pom.xml"), "<artifactId>a</artifactId>");
        Verifier verifier = build(root, "package", 4);
        verifier.addCliArgument(GROUP + ":" + PROBE + ":" + VERSION + ":aggregate");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("aggregate (default-cli)");
        assertTrue(Files.exists(root.resolve("target/checked.txt")));
    }

    private String dependency(String artifact, String type, String extra) {
        return "<dependency><groupId>" + GROUP + "</groupId><artifactId>" + artifact + "</artifactId><version>" + VERSION
                + "</version><type>" + type + "</type>" + extra + "</dependency>";
    }

    private Path extractPlugin() throws Exception {
        Path source = extractResources("/mng-13763").toPath().resolve("plugin");
        Path dir = Files.createTempDirectory(source.getParent().getParent(), "mng-13763-ordering-");
        Path plugin = dir.resolve("plugin");
        try (var paths = Files.walk(source.resolve("src"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Path copy = plugin.resolve(source.relativize(path));
                Files.createDirectories(copy.getParent());
                Files.copy(path, copy);
            }
        }
        Files.writeString(plugin.resolve("pom.xml"), Files.readString(source.resolve("pom.xml"))
                .replace("<artifactId>aggregator-probe-maven-plugin</artifactId>", "<artifactId>" + PROBE + "</artifactId>"));
        return dir;
    }

    private Path preparePlugin() throws Exception {
        Path dir = extractPlugin();
        installPlugin(dir);
        return dir;
    }

    private void installPlugin(Path dir) throws Exception {
        Verifier verifier = newVerifier(dir.resolve("plugin").toFile().getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    private Verifier build(Path root, String phase) throws Exception {
        return build(root, phase, 1);
    }

    private Verifier build(Path root, String phase, int threads) throws Exception {
        Verifier verifier = newVerifier(root.toFile().getAbsolutePath());
        try (var paths = Files.walk(root)) {
            for (Path target : paths.filter(p -> p.getFileName().toString().equals("target")).toList()) {
                verifier.deleteDirectory(root.relativize(target).toString());
            }
        }
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T" + threads);
        verifier.addCliArgument(phase);
        return verifier;
    }

    private void project(Path dir, String parent, String body) throws Exception {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("pom.xml"), "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
                + parent + (parent.isEmpty() ? "<groupId>" + GROUP + "</groupId><version>" + VERSION + "</version>" : "")
                + "<packaging>pom</packaging>" + body + "</project>");
    }

    private String parent(String artifact, String relativePath) {
        return "<parent><groupId>" + GROUP + "</groupId><artifactId>" + artifact + "</artifactId><version>" + VERSION
                + "</version><relativePath>" + relativePath + "</relativePath></parent>";
    }

    private String plugin(String version, String inheritance, String executions) {
        return "<build><plugins><plugin><groupId>" + GROUP + "</groupId><artifactId>" + PROBE + "</artifactId><version>"
                + version + "</version>" + inheritance + "<executions>" + executions + "</executions></plugin></plugins></build>";
    }

    private String execution(String id, String phase, String goals, String extra) {
        return "<execution><id>" + id + "</id>" + (phase == null ? "" : "<phase>" + phase + "</phase>")
                + extra + "<goals><goal>" + goals + "</goal></goals></execution>";
    }
}
