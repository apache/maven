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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3506">MNG-3506</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng3506ArtifactHandlersFromPluginsTest extends AbstractMavenIntegrationTestCase {

    private static final String GID = "org.apache.maven.its.mng3506";
    private static final String AID = "mng-3506";
    private static final String VERSION = "1";
    private static final String TYPE = "jar";
    private static final String BAD_TYPE1 = "coreit-1";
    private static final String BAD_TYPE2 = "coreit-2";

    public MavenITmng3506ArtifactHandlersFromPluginsTest() {
        super("(2.2.0,)");
    }

    @Test
    public void testProjectPackagingUsage() throws IOException, VerificationException {
        File testDir = extractResources("/" + AID);

        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        verifier.deleteArtifacts(GID, null);

        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        // Now, if everything worked, we have .pom and a .jar in the local repo for each child, and a pom for the
        // parent.
        // IF IT DIDN'T, we have a .pom and a .coreit-1 for child 1 AND/OR .pom and .coreit-2 for child 2 in the local
        // repo...

        // Parent POM
        String path = verifier.getArtifactPath(GID, AID, VERSION, "pom", null);
        assertTrue(new File(path).exists(), path + " should have been installed.");

        // Child 1
        path = verifier.getArtifactPath(GID, AID + ".1", VERSION, TYPE, null);
        assertTrue(new File(path).exists(), path + " should have been installed.");

        path = verifier.getArtifactPath(GID, AID + ".1", VERSION, "pom", null);
        assertTrue(new File(path).exists(), path + " should have been installed.");

        path = verifier.getArtifactPath(GID, AID + ".1", VERSION, BAD_TYPE1, null);
        assertFalse(new File(path).exists(), path + " should NOT have been installed.");

        path = verifier.getArtifactPath(GID, AID + ".1", VERSION, BAD_TYPE2, null);
        assertFalse(new File(path).exists(), path + " should _NEVER_ be installed!!!");

        // Child 2
        path = verifier.getArtifactPath(GID, AID + ".2", VERSION, TYPE, null);
        assertTrue(new File(path).exists(), path + " should have been installed.");

        path = verifier.getArtifactPath(GID, AID + ".2", VERSION, "pom", null);
        assertTrue(new File(path).exists(), path + " should have been installed.");

        path = verifier.getArtifactPath(GID, AID + ".2", VERSION, BAD_TYPE1, null);
        assertFalse(new File(path).exists(), path + " should _NEVER_ be installed!!!");

        path = verifier.getArtifactPath(GID, AID + ".2", VERSION, BAD_TYPE2, null);
        assertFalse(new File(path).exists(), path + " should NOT have been installed.");
    }
}
