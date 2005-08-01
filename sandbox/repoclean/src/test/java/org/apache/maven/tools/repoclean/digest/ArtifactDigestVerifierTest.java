package org.apache.maven.tools.repoclean.digest;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.tools.repoclean.TestSupport;
import org.apache.maven.tools.repoclean.report.DummyReporter;
import org.apache.maven.tools.repoclean.transaction.RewriteTransaction;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

public class ArtifactDigestVerifierTest
    extends PlexusTestCase
{
    private ArtifactFactory artifactFactory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    public void testShouldWriteBothMD5AndSHA1DigestFiles()
        throws Exception
    {
        DigestVerifier verifier = (DigestVerifier) lookup( DigestVerifier.ROLE );

        Artifact artifact = artifactFactory.createBuildArtifact( "testGroup", "testArtifact", "1.0", "jar" );

        File artifactFile = TestSupport.getResource( "digest/ArtifactDigestorTest/digestFormatVerifyArtifact.jar" );

        artifact.setFile( artifactFile );

        File tempFile = File.createTempFile( "artifactDigestFileVerifyBase", "jar" );

        File md5 = new File( tempFile + ".md5" );
        File sha1 = new File( tempFile + ".sha1" );

        System.out.println( "[INFO] We expect warnings for missing source digest files here:" );
        verifier.verifyDigest( artifactFile, tempFile, new RewriteTransaction( artifact ), new DummyReporter(), false );
        System.out.println( "[INFO] Target digest files should have been created." );

        assertTrue( md5.exists() );
        assertTrue( sha1.exists() );
    }

}
