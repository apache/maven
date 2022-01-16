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

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2362">MNG-2362</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2362DeployedPomEncodingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2362DeployedPomEncodingTest()
    {
        super( "[2.0.5,)" );
    }

    /**
     * Verify that installed/deployed POMs retain their original file encoding and don't get messed up by some
     * transformation that erroneously uses the platform's default encoding for reading/writing them.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2362" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "utf-8/target" );
        verifier.deleteDirectory( "latin-1/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2362" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File pomFile;

        pomFile = new File( verifier.getArtifactPath( "org.apache.maven.its.mng2362", "utf-8", "0.1", "pom" ) );
        assertPomUtf8( pomFile );

        pomFile = new File( testDir, "utf-8/target/repo/org/apache/maven/its/mng2362/utf-8/0.1/utf-8-0.1.pom" );
        assertPomUtf8( pomFile );

        pomFile = new File( verifier.getArtifactPath( "org.apache.maven.its.mng2362", "latin-1", "0.1", "pom" ) );
        assertPomLatin1( pomFile );

        pomFile = new File( testDir, "latin-1/target/repo/org/apache/maven/its/mng2362/latin-1/0.1/latin-1-0.1.pom" );
        assertPomLatin1( pomFile );
    }

    private void assertPomUtf8( File pomFile )
        throws Exception
    {
        String pom = FileUtils.fileRead( pomFile, "UTF-8" );
        String chars = "\u00DF\u0131\u03A3\u042F\u05D0\u20AC";
        assertPom( pomFile, pom, chars );
    }

    private void assertPomLatin1( File pomFile )
        throws Exception
    {
        String pom = FileUtils.fileRead( pomFile, "ISO-8859-1" );
        String chars = "\u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF";
        assertPom( pomFile, pom, chars );
    }

    private void assertPom( File pomFile, String pom, String chars )
        throws Exception
    {
        String prefix = "TEST-CHARS: ";
        int pos = pom.indexOf( prefix );
        assertTrue( "Corrupt data " + pom.substring( pos, pos + prefix.length() + chars.length() ) + " in " + pomFile,
                    pom.contains( prefix + chars ) );
    }

}
