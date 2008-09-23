package org.apache.maven.integrationtests;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Tests that artifact checksums are properly verified.
 */
public class MavenITmng2744checksumVerificationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2744checksumVerificationTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    /**
     * Tests that hex digits are compared without regard to case.
     */
    public void testitMNG2744()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2744-checksumVerification" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "a", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "a", "1", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "b", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "b", "1", "jar" );

        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
