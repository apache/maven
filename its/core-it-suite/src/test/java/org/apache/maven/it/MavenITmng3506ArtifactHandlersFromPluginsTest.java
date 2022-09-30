package org.apache.maven.it;

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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3506">MNG-3506</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng3506ArtifactHandlersFromPluginsTest
    extends AbstractMavenIntegrationTestCase
{

    private static final String GID = "org.apache.maven.its.mng3506";
    private static final String AID = "mng-3506";
    private static final String VERSION = "1";
    private static final String TYPE = "jar";
    private static final String BAD_TYPE1 = "coreit-1";
    private static final String BAD_TYPE2 = "coreit-2";

    public MavenITmng3506ArtifactHandlersFromPluginsTest()
    {
        super( "(2.2.0,)" );
    }

    @Test
    public void testProjectPackagingUsage()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/" + AID );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );

        verifier.deleteArtifacts( GID );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Now, if everything worked, we have .pom and a .jar in the local repo for each child, and a pom for the parent.
        // IF IT DIDN'T, we have a .pom and a .coreit-1 for child 1 AND/OR .pom and .coreit-2 for child 2 in the local repo...

        // Parent POM
        String path = verifier.getArtifactPath( GID, AID, VERSION, "pom" );
        assertTrue( path + " should have been installed.", new File( path ).exists() );

        // Child 1
        path = verifier.getArtifactPath( GID, AID + ".1", VERSION, TYPE );
        assertTrue( path + " should have been installed.", new File( path ).exists() );

        path = verifier.getArtifactPath( GID, AID + ".1", VERSION, "pom" );
        assertTrue( path + " should have been installed.", new File( path ).exists() );

        path = verifier.getArtifactPath( GID, AID + ".1", VERSION, BAD_TYPE1 );
        assertFalse( path + " should NOT have been installed.", new File( path ).exists() );

        path = verifier.getArtifactPath( GID, AID + ".1", VERSION, BAD_TYPE2 );
        assertFalse( path + " should _NEVER_ be installed!!!", new File( path ).exists() );

        // Child 2
        path = verifier.getArtifactPath( GID, AID + ".2", VERSION, TYPE );
        assertTrue( path + " should have been installed.", new File( path ).exists() );

        path = verifier.getArtifactPath( GID, AID + ".2", VERSION, "pom" );
        assertTrue( path + " should have been installed.", new File( path ).exists() );

        path = verifier.getArtifactPath( GID, AID + ".2", VERSION, BAD_TYPE1 );
        assertFalse( path + " should _NEVER_ be installed!!!", new File( path ).exists() );

        path = verifier.getArtifactPath( GID, AID + ".2", VERSION, BAD_TYPE2 );
        assertFalse( path + " should NOT have been installed.", new File( path ).exists() );
    }
}
