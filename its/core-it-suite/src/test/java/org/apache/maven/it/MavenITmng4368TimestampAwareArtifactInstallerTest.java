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

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4368">MNG-4368</a>.
 * Resolver 2.0.0 (in use since Maven 4.0.0-alpha-9) undoes this "smart" solution.
 *
 * @author Benjamin Bentmann
 */
@Disabled("Bounds: [2.0.3,3.0-alpha-1),[3.0-alpha-6,4.0.0-alpha-8]")
public class MavenITmng4368TimestampAwareArtifactInstallerTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that the artifact installer copies POMs to the local repo even if they have an older timestamp as the
     * copy in the local repo.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPomPackaging() throws Exception {
        Path testDir = extractResources("mng-4368/pom");

        Path aDir = testDir.resolve("branch-a");
        Path aPom = aDir.resolve("pom.xml");
        Path bDir = testDir.resolve("branch-b");
        Path bPom = bDir.resolve("pom.xml");

        ItUtils.lastModified(aPom, System.currentTimeMillis());
        ItUtils.lastModified(bPom, ItUtils.lastModified(aPom) - 1000 * 60);

        Verifier verifier = newVerifier(aDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4368");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path installedPom =
                verifier.getArtifactPath("org.apache.maven.its.mng4368", "test", "0.1-SNAPSHOT", "pom");

        String pom = Files.readString(installedPom);
        assertTrue(pom.indexOf("Branch-A") > 0);
        assertFalse(pom.contains("Branch-B"));

        assertEquals(Files.size(aPom), Files.size(bPom));
        assertTrue(ItUtils.lastModified(aPom) > ItUtils.lastModified(bPom));
        assertTrue(ItUtils.lastModified(installedPom) > ItUtils.lastModified(bPom));

        verifier = newVerifier(bDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        pom = Files.readString(installedPom);
        assertFalse(pom.contains("Branch-A"));
        assertTrue(pom.indexOf("Branch-B") > 0);
    }

    /**
     * Verify that the artifact installer copies files to the local repo only if their timestamp differs from the copy
     * already in the local repo.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitJarPackaging() throws Exception {
        // requiresMavenVersion("[2.2.2,3.0-alpha-1),[3.0-alpha-6,)");

        Path testDir = extractResources("mng-4368/jar");

        Path aDir = testDir.resolve("branch-a");
        Path aArtifact = aDir.resolve("artifact.jar");
        Path bDir = testDir.resolve("branch-b");
        Path bArtifact = bDir.resolve("artifact.jar");

        Files.writeString(aArtifact, "from Branch-A");
        ItUtils.lastModified(aArtifact, System.currentTimeMillis());
        Files.writeString(bArtifact, "from Branch-B");
        ItUtils.lastModified(bArtifact, ItUtils.lastModified(aArtifact) - 1000 * 60);

        Verifier verifier = newVerifier(aDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4368");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path installedArtifact =
                verifier.getArtifactPath("org.apache.maven.its.mng4368", "test", "0.1-SNAPSHOT", "jar");

        String data = Files.readString(installedArtifact);
        assertTrue(data.indexOf("Branch-A") > 0);
        assertFalse(data.contains("Branch-B"));

        assertEquals(Files.size(aArtifact), Files.size(bArtifact));
        assertTrue(ItUtils.lastModified(aArtifact) > ItUtils.lastModified(bArtifact));
        assertTrue(ItUtils.lastModified(installedArtifact) > ItUtils.lastModified(bArtifact));

        verifier = newVerifier(bDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        data = Files.readString(installedArtifact);
        assertFalse(data.contains("Branch-A"));
        assertTrue(data.indexOf("Branch-B") > 0);

        long lastModified = ItUtils.lastModified(installedArtifact);
        Files.writeString(installedArtifact, "from Branch-C");
        ItUtils.lastModified(installedArtifact, lastModified);

        verifier = newVerifier(bDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-b.txt");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        data = Files.readString(installedArtifact);
        assertFalse(data.contains("Branch-B"));
        assertTrue(data.indexOf("Branch-C") > 0);
    }
}
