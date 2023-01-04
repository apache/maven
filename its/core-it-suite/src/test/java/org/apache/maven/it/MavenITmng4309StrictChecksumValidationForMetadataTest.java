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

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4309">MNG-4309</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4309StrictChecksumValidationForMetadataTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4309StrictChecksumValidationForMetadataTest()
    {
        super( "[3.0-beta-3,)" );
    }

    /**
     * Verify that strict checksum verification applies to metadata as well and in particular fails the build
     * during deployment when the previous metadata is corrupt.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4309" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4309" );
        FileUtils.copyDirectoryStructure( new File( testDir, "repo" ), new File( testDir, "target/repo" ) );
        verifier.addCliOption( "--strict-checksums" );
        try
        {
          verifier.executeGoal( "validate" );
          verifier.verifyErrorFreeLog();
          fail( "Checksum mismatch for metadata did not fail the build despite strict mode" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
    }

}
