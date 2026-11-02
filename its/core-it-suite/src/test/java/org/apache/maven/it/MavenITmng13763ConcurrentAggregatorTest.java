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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MavenITmng13763ConcurrentAggregatorTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testAggregatorWaitsForChildren() throws Exception {
        Path testDir = extractResources("mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir.resolve("reactor"), true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertEquals(
                "a=true b=true aa=true",
                Files.readString(testDir.resolve("reactor/target/aggregate-observed.txt")).trim());
        assertFalse(Files.exists(testDir.resolve("reactor/a/target/aggregate-observed.txt")));
        assertFalse(Files.exists(testDir.resolve("reactor/a/aa/target/aggregate-observed.txt")));
        assertFalse(Files.exists(testDir.resolve("reactor/b/target/aggregate-observed.txt")));
    }

    @Test
    public void testAggregatorDoesNotRunAfterChildFailure() throws Exception {
        Path testDir = extractResources("mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir.resolve("reactor"), true);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("--fail-at-end");
        verifier.addCliArgument("-DfailModule=b");
        verifier.addCliArgument("package");
        assertThrows(VerificationException.class, verifier::execute);

        assertFalse(Files.exists(testDir.resolve("reactor/target/aggregate-observed.txt")));
    }

    @Test
    public void testChildProfileAggregatorIsPreserved() throws Exception {
        Path testDir = extractResources("mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir.resolve("reactor"), true);
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
                Files.readString(testDir.resolve("reactor/target/aggregate-observed.txt")).trim());
        assertEquals(
                "a=false b=false aa=true",
                Files.readString(testDir.resolve("reactor/a/target/aggregate-observed.txt")).trim());
    }

    @Test
    public void testManagedAggregatorRunsInModules() throws Exception {
        Path testDir = extractResources("mng-13763");
        installPlugin(testDir);

        Verifier verifier = newCleanVerifier(testDir.resolve("managed"), false);
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-DrequireOwnTouch=true");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertFalse(Files.exists(testDir.resolve("managed/target/aggregate-observed.txt")));
        assertEquals(
                "a=false b=false aa=false",
                Files.readString(testDir.resolve("managed/a/target/aggregate-observed.txt")).trim());
        assertEquals(
                "a=false b=false aa=false",
                Files.readString(testDir.resolve("managed/b/target/aggregate-observed.txt")).trim());
    }

    private void installPlugin(Path testDir) throws Exception {
        Verifier verifier = newVerifier(testDir.resolve("plugin"));
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    private Verifier newCleanVerifier(Path reactorDir, boolean nested) throws IOException, VerificationException {
        Verifier verifier = newVerifier(reactorDir);
        for (String path : new String[] {"target", "a/target", "b/target"}) {
            verifier.deleteDirectory(path);
        }
        if (nested) {
            verifier.deleteDirectory("a/aa/target");
        }
        return verifier;
    }
}
