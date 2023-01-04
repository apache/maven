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

import java.io.File;
import java.util.Locale;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3892">MNG-3892</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3892ReleaseDeploymentTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3892ReleaseDeploymentTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that a bunch of release artifacts can be deployed without the deployer erroneously complaining about
     * already deployed artifacts.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3892()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3892" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "repo" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3892" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng3892", "test", "1.0", "pom" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng3892", "test", "1.0", "jar" );

        String groupDir = "repo/org/apache/maven/its/mng3892/test/";
        verifier.verifyFilePresent( groupDir + "maven-metadata.xml" );
        verifier.verifyFilePresent( groupDir + "maven-metadata.xml.md5" );
        verifier.verifyFilePresent( groupDir + "maven-metadata.xml.sha1" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0.pom" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0.pom.md5" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0.pom.sha1" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0.jar" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0.jar.md5" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0.jar.sha1" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0-it.jar" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0-it.jar.md5" );
        verifier.verifyFilePresent( groupDir + "1.0/test-1.0-it.jar.sha1" );

        verify( testDir, groupDir + "1.0/test-1.0.jar.md5", "dd89c30cc71c3cd8a729622243c76770" );
        verify( testDir, groupDir + "1.0/test-1.0.jar.sha1", "0b0717ff89d3cbadc3564270bf8930163753bf71" );
        verify( testDir, groupDir + "1.0/test-1.0-it.jar.md5", "dd89c30cc71c3cd8a729622243c76770" );
        verify( testDir, groupDir + "1.0/test-1.0-it.jar.sha1", "0b0717ff89d3cbadc3564270bf8930163753bf71" );
    }

    private void verify( File testDir, String file, String checksum )
        throws Exception
    {
        assertEquals( file, checksum, readChecksum( new File( testDir, file ) ) );
    }

    private String readChecksum( File checksumFile )
        throws Exception
    {
        String checksum = FileUtils.fileRead( checksumFile, "UTF-8" ).trim();
        if ( checksum.indexOf( ' ' ) >= 0 )
        {
            checksum = checksum.substring( 0, checksum.indexOf( ' ' ) );
        }
        return checksum.toLowerCase( Locale.ENGLISH );
    }

}
