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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3379">MNG-3379</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3379ParallelArtifactDownloadsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3379ParallelArtifactDownloadsTest()
    {
        super( "[2.0.5,3.0-alpha-1),[3.0-alpha-2,)" );
    }

    /**
     * Tests that parallel downloads of artifacts from both the same and from different group ids don't corrupt
     * the local repo.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3379()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3379" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.a" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.b" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.c" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.d" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.setSystemProperty( "maven.artifact.threads", "16" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String gid = "org.apache.maven.its.mng3379.";
        assertArtifact( verifier, gid + "a", "x", "0.2-SNAPSHOT", "", "jar",
            "69c041c12f35894230c7c23c49cd245886c6fb6f" );
        assertArtifact( verifier, gid + "a", "x", "0.2-SNAPSHOT", "", "pom",
            "f0abcb2aa6d99f045c013ecb2671a3a3e71bd715" );
        assertArtifact( verifier, gid + "a", "x", "0.2-SNAPSHOT", "tests", "jar",
            "69c041c12f35894230c7c23c49cd245886c6fb6f" );
        assertArtifact( verifier, gid + "a", "x", "0.2-SNAPSHOT", "sources", "jar",
            "166f8bef02b9e92f99ec3b163d8321dd1d087e34" );
        assertArtifact( verifier, gid + "a", "x", "0.2-SNAPSHOT", "javadoc", "jar",
            "4d96e09f7e93870685a317c574f851b407224415" );
        assertMetadata( verifier, gid + "a", "x", "0.2-SNAPSHOT",
            "e1cfc3a77657fc46bb624dee25c61b290e5b4dd7" );

        assertArtifact( verifier, gid + "b", "x", "0.2-SNAPSHOT", "", "jar",
            "efb7c4046565774cd7e44645e02f06ecdf91098d" );
        assertArtifact( verifier, gid + "b", "x", "0.2-SNAPSHOT", "", "pom",
            "a057baebe5cdae3978b530c0bfea8b523b3d4506" );
        assertArtifact( verifier, gid + "b", "x", "0.2-SNAPSHOT", "tests", "jar",
            "efb7c4046565774cd7e44645e02f06ecdf91098d" );
        assertArtifact( verifier, gid + "b", "x", "0.2-SNAPSHOT", "sources", "jar",
            "9ad231fc04ea1114987c377cc5cbccfbf83e3dbf" );
        assertArtifact( verifier, gid + "b", "x", "0.2-SNAPSHOT", "javadoc", "jar",
            "7807daefd3af3be73d3b92f9c5ab1b52510c0767" );
        assertMetadata( verifier, gid + "b", "x", "0.2-SNAPSHOT",
            "5ccc4edfb503f9a5ccadedf102dff8943250d830" );
        assertMetadata( verifier, gid + "b", "x",
            "8f38b1041871f22dcb031544d8a3436c335bfcdb" );

        assertArtifact( verifier, gid + "c", "x", "0.2-SNAPSHOT", "", "jar",
            "1eb0d5a421b3074e8a69b0dcca7e325c0636a932" );
        assertArtifact( verifier, gid + "c", "x", "0.2-SNAPSHOT", "", "pom",
            "9c993bdebc7bd1b673891f203511fed9085996f3" );
        assertArtifact( verifier, gid + "c", "x", "0.2-SNAPSHOT", "tests", "jar",
            "1eb0d5a421b3074e8a69b0dcca7e325c0636a932" );
        assertArtifact( verifier, gid + "c", "x", "0.2-SNAPSHOT", "sources", "jar",
            "82f9664b3a910fb861fc4ed2b79e39d8f95e3675" );
        assertArtifact( verifier, gid + "c", "x", "0.2-SNAPSHOT", "javadoc", "jar",
            "64a3bfe19b294f67b1c52a2514c58922b88e5f97" );
        assertMetadata( verifier, gid + "c", "x", "0.2-SNAPSHOT",
            "b31ef40a51bdab4e6e44bfe3f2d1da42e5e42e46" );
        assertMetadata( verifier, gid + "c", "x",
            "c4848e60d226ec6304df3abd9eba8fdb301b3660" );

        assertArtifact( verifier, gid + "d", "x", "0.2-SNAPSHOT", "", "jar",
            "3d606c564625a594165bcbbe4a24c8f11b18b5a0" );
        assertArtifact( verifier, gid + "d", "x", "0.2-SNAPSHOT", "", "pom",
            "e7b6322cea42970e61316b161f79da690f042f7e" );
        assertArtifact( verifier, gid + "d", "x", "0.2-SNAPSHOT", "tests", "jar",
            "3d606c564625a594165bcbbe4a24c8f11b18b5a0" );
        assertArtifact( verifier, gid + "d", "x", "0.2-SNAPSHOT", "sources", "jar",
            "35a7e140307f4bb67984dc72aa551f0faabacd36" );
        assertArtifact( verifier, gid + "d", "x", "0.2-SNAPSHOT", "javadoc", "jar",
            "2fe3487f496fe66f23772b1bada066ec6bd9222f" );
        assertMetadata( verifier, gid + "d", "x", "0.2-SNAPSHOT",
            "a0d0b5efd5d6f6a921a3f7c1a6a503359fccef04" );
        assertMetadata( verifier, gid + "d", "x",
            "1d2bf926862f2131f1229328e588b906b087bdb3" );
    }

    private void assertArtifact( Verifier verifier, String gid, String aid, String ver, String cls, String ext, String sha1 )
        throws Exception
    {
        File file = new File( verifier.getArtifactPath( gid, aid, ver, ext, cls ) );
        assertTrue( file.getAbsolutePath(), file.isFile() );
        assertEquals( sha1, ItUtils.calcHash( file, "SHA-1" ) );
    }

    private void assertMetadata( Verifier verifier, String gid, String aid, String ver, String sha1 )
        throws Exception
    {
        String name = "maven-metadata-maven-core-it.xml";
        File file = new File( verifier.getArtifactMetadataPath( gid, aid, ver, name ) );
        assertTrue( file.getAbsolutePath(), file.isFile() );
        assertEquals( sha1, ItUtils.calcHash( file, "SHA-1" ) );
    }

    private void assertMetadata( Verifier verifier, String gid, String aid, String sha1 )
        throws Exception
    {
        String name = "maven-metadata-maven-core-it.xml";
        File file = new File( verifier.getArtifactMetadataPath( gid, aid, null, name ) );
        assertTrue( file.getAbsolutePath(), file.isFile() );
        assertEquals( sha1, ItUtils.calcHash( file, "SHA-1" ) );
    }

}
