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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenITmng13763ConcurrentAggregatorTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng13763ConcurrentAggregatorTest() {
        super("[4.0.0-rc-1,)");
    }

    @Test
    public void testAggregatorWaitsForChildren() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir, "reactor", true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertEquals(
                "a=true b=true aa=true",
                Files.readString(testDir.toPath().resolve("reactor/target/aggregate-observed.txt"))
                        .trim());
        assertFalse(Files.exists(testDir.toPath().resolve("reactor/a/target/aggregate-observed.txt")));
        assertFalse(Files.exists(testDir.toPath().resolve("reactor/a/aa/target/aggregate-observed.txt")));
        assertFalse(Files.exists(testDir.toPath().resolve("reactor/b/target/aggregate-observed.txt")));
    }

    @Test
    public void testAggregatorDoesNotRunAfterChildFailure() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir, "reactor", true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("--fail-at-end");
        verifier.addCliArgument("-DfailModule=b");
        verifier.addCliArgument("package");
        assertThrows(VerificationException.class, verifier::execute);

        assertFalse(Files.exists(testDir.toPath().resolve("reactor/target/aggregate-observed.txt")));
    }

    @Test
    public void testChildProfileAggregatorIsPreserved() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir, "reactor", true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-Pchild-aggregate");
        verifier.addCliArgument("-DexpectChildAggregator=true");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertEquals(
                "a=true b=true aa=true",
                Files.readString(testDir.toPath().resolve("reactor/target/aggregate-observed.txt"))
                        .trim());
        assertEquals(
                "a=false b=false aa=true",
                Files.readString(testDir.toPath().resolve("reactor/a/target/aggregate-observed.txt"))
                        .trim());
    }

    @Test
    public void testPartialBuildPreservesInheritedAggregator() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir, "reactor", true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-pl");
        verifier.addCliArgument(":a");
        verifier.addCliArgument("-DrequireOwnTouch=true");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue(Files.exists(testDir.toPath().resolve("reactor/a/target/aggregate-observed.txt")));
        assertTrue(Files.exists(testDir.toPath().resolve("reactor/a/target/touched.txt")));
        assertFalse(Files.exists(testDir.toPath().resolve("reactor/target/aggregate-observed.txt")));
        assertFalse(Files.exists(testDir.toPath().resolve("reactor/b/target/aggregate-observed.txt")));
    }

    @Test
    public void testManagedAggregatorRunsInModules() throws Exception {
        verifyManagedAggregator(null);
    }

    @Test
    public void testManagedAggregatorWithNoninheritedPlugin() throws Exception {
        verifyManagedAggregator("noninherited-plugin");
    }

    @Test
    public void testManagedAggregatorWithNoninheritedExecution() throws Exception {
        verifyManagedAggregator("noninherited-execution");
    }

    private void verifyManagedAggregator(String profile) throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir, "managed", false);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-DrequireOwnTouch=" + (profile == null));
        if (profile != null) {
            verifier.addCliArgument("-P" + profile);
        }
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertEquals(profile != null, Files.exists(testDir.toPath().resolve("managed/target/aggregate-observed.txt")));
        assertEquals(
                "a=false b=false aa=false",
                Files.readString(testDir.toPath().resolve("managed/a/target/aggregate-observed.txt"))
                        .trim());
        assertEquals(
                "a=false b=false aa=false",
                Files.readString(testDir.toPath().resolve("managed/b/target/aggregate-observed.txt"))
                        .trim());
    }

    private void installPlugin(File testDir) throws Exception {
        Verifier verifier = newVerifier(new File(testDir, "plugin").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSettingsProfileDoesNotActivatePomDeclaration() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);
        Verifier verifier = newCleanVerifier(testDir, "reactor", true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("--settings=" + new File(testDir, "settings.xml").getAbsolutePath());
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertFalse(Files.exists(testDir.toPath().resolve("reactor/a/target/aggregate-observed.txt")));
    }

    @Test
    public void testInheritanceStopsAtIntermediatePlugin() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);
        Verifier verifier = newCleanVerifier(testDir, "managed-barrier", true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertTrue(Files.exists(testDir.toPath().resolve("managed-barrier/a/aa/target/aggregate-observed.txt")));
        assertFalse(Files.exists(testDir.toPath().resolve("managed-barrier/a/target/aggregate-observed.txt")));
    }

    @Test
    public void testConsumerWaitsForAttachedAggregate() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);
        Path reactor = testDir.toPath().resolve("outputs");
        Verifier verifier = newVerifier(reactor.toFile().getAbsolutePath());
        for (String path : new String[] {"target", "group/target", "group/a/target", "consumer/target"}) {
            verifier.deleteDirectory(path);
        }
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T4");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertTrue(Files.exists(reactor.resolve("group/target/aggregate.jar")));
        verifier.verifyTextInLog("Aggregate artifact resolved");
    }

    @Test
    public void testAggregatorForSubprojectWithDifferentParent() throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);
        Verifier verifier = newVerifier(new File(testDir, "nonparent").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("a/target");
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertTrue(Files.exists(testDir.toPath().resolve("nonparent/a/target/aggregate-observed.txt")));
    }

    @Test
    public void testExplicitAggregatorWithInterpolatedCoordinatesAndGoal() throws Exception {
        verifyInterpolatedAggregator("properties");
    }

    @Test
    public void testProfileAggregatorMergedWithAnotherGoal() throws Exception {
        verifyInterpolatedAggregator("profile-properties");
    }

    private void verifyInterpolatedAggregator(String fixture) throws Exception {
        File testDir = extractResources("/mng-13763");
        installPlugin(testDir);
        Verifier verifier = newVerifier(new File(testDir, fixture).getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("a/target");
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T1");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertTrue(Files.exists(testDir.toPath().resolve(fixture + "/a/target/aggregate-observed.txt")));
    }

    private Verifier newCleanVerifier(File testDir, String subdir, boolean nested)
            throws IOException, VerificationException {
        Verifier verifier = newVerifier(new File(testDir, subdir).getAbsolutePath());
        for (String path : new String[] {"target", "a/target", "b/target"}) {
            verifier.deleteDirectory(path);
        }
        if (nested) {
            verifier.deleteDirectory("a/aa/target");
        }
        return verifier;
    }
}
