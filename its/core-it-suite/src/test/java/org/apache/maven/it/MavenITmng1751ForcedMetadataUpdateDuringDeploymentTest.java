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
import java.util.Properties;

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;

import static org.junit.Assert.assertNotEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1751">MNG-1751</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1751ForcedMetadataUpdateDuringDeploymentTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1751ForcedMetadataUpdateDuringDeploymentTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that deployment always updates the metadata even if its remote timestamp currently refers to
     * the future.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1751" );

        File dir = new File( testDir, "repo/org/apache/maven/its/mng1751/dep/0.1-SNAPSHOT" );
        File templateMetadataFile = new File( dir, "template-metadata.xml" );
        File metadataFile = new File( dir, "maven-metadata.xml" );
        FileUtils.copyFile( templateMetadataFile, metadataFile );
        String checksum = ItUtils.calcHash( metadataFile, "SHA-1" );
        FileUtils.fileWrite( metadataFile.getPath() + ".sha1", checksum );

        // phase 1: deploy a new snapshot, this should update the metadata despite its future timestamp
        Verifier verifier = newVerifier( new File( testDir, "dep" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng1751" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // phase 2: resolve snapshot, if the previous deployment didn't update the metadata, we get the wrong file
        verifier = newVerifier( new File( testDir, "test" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng1751" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties checksums = verifier.loadProperties( "target/checksum.properties" );
        String sha1 = checksums.getProperty( "dep-0.1-SNAPSHOT.jar", "" ).toLowerCase( java.util.Locale.ENGLISH );
        assertEquals( sha1, 40, sha1.length() );
        assertNotEquals( "fc081cd365b837dcb01eb9991f21c409b155ea5c", sha1 );
    }

}
