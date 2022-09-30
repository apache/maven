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

import org.junit.jupiter.api.Test;

public class MavenITmng7128BlockExternalHttpReactorTest
        extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7128-block-external-http-reactor";

    public MavenITmng7128BlockExternalHttpReactorTest()
    {
        super( "[3.8.1,)" );
    }

    /**
     * This test verifies that defining a repository in pom.xml that uses HTTP is blocked.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBlockedHttpRepositoryInPom() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        // ITs override global settings that provide blocked mirror: need to define the mirror in dedicated settings
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        try
        {
            verifier.executeGoal( "compiler:compile" );
            fail( "HTTP repository defined in pom.xml should have failed the build but did not." );
        }
        catch ( VerificationException ve )
        {
            // Inspect the reason why the build broke.
            verifier.verifyTextInLog( "[ERROR] Failed to execute goal on project http-repository-in-pom: "
                + "Could not resolve dependencies for project org.apache.maven.its.mng7128:http-repository-in-pom:jar:1.0: "
                + "Failed to collect dependencies at junit:junit:jar:1.3: "
                + "Failed to read artifact descriptor for junit:junit:jar:1.3: "
                + "Could not transfer artifact junit:junit:pom:1.3 from/to maven-default-http-blocker (http://0.0.0.0/): " // mirror introduced in MNG-7118
                + "Blocked mirror for repositories: [insecure-http-repo (http://repo.maven.apache.org/, default, releases+snapshots)]" );
        }
    }
}
