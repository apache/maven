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

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2744">MNG-2744</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2744checksumVerificationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2744checksumVerificationTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    /**
     * Tests that hex digits of checksums are compared without regard to case.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG2744()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2744" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2744" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng2744", "a", "1", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng2744", "a", "1", "pom" );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng2744", "b", "1", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng2744", "b", "1", "pom" );
    }

}
