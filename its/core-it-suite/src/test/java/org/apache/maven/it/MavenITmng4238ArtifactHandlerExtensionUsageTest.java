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
public class MavenITmng4238ArtifactHandlerExtensionUsageTest extends AbstractMavenIntegrationTestCase {

    private static final String GID = "org.apache.maven.its.mng4238";
    private static final String AID = "mng-4238";
    private static final String VERSION = "1";
    private static final String TYPE = "jar";
    private static final String BAD_TYPE = "coreit";

    public MavenITmng4238ArtifactHandlerExtensionUsageTest() {
        super("(2.2.0,)");
    }

    @Test
    public void testProjectPackagingUsage() throws IOException, VerificationException {
        File testDir = extractResources("/mng-4238");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        verifier.deleteArtifacts(GID);

        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        // Now, if everything worked, we have a .pom and a .jar in the local repo.
        // IF IT DIDN'T, we have a .pom and a .coreit in the local repo...

        String path = verifier.getArtifactPath(GID, AID, VERSION, TYPE);
        assertTrue(new File(path).exists(), path + " should have been installed.");

        path = verifier.getArtifactPath(GID, AID, VERSION, "pom");
        assertTrue(new File(path).exists(), path + " should have been installed.");

        path = verifier.getArtifactPath(GID, AID, VERSION, BAD_TYPE);
        assertFalse(new File(path).exists(), path + " should NOT have been installed.");
    }
}
