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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Downloads a snapshot dependency that was deployed with uniqueVersion = false, and checks it can be
 * updated. See <a href="https://issues.apache.org/jira/browse/MNG-1908">MNG-1908</a>.
 */
@Disabled( "flaky test, see MNG-3137" )
public class MavenIT0108SnapshotUpdateTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0108SnapshotUpdateTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    private Verifier verifier;

    private File artifact;

    private File repository;

    private File localRepoFile;

    private static final int TIME_OFFSET = 50000;

    @BeforeEach
    protected void setUp()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0108" );
        verifier = newVerifier( testDir.getAbsolutePath() );
        localRepoFile = getLocalRepoFile( verifier );
        deleteLocalArtifact( verifier, localRepoFile );

        repository = new File( testDir, "repository" );
        recreateRemoteRepository( repository );

        // create artifact in repository (TODO: into verifier)
        artifact = new File( repository,
                             "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-core-it-support-1.0-SNAPSHOT.jar" );
        artifact.getParentFile().mkdirs();
        FileUtils.fileWrite( artifact.getAbsolutePath(), "originalArtifact" );

        verifier.verifyArtifactNotPresent( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar" );
    }

    @Test
    public void testSnapshotUpdated()
        throws Exception
    {
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();

        verifyArtifactContent( "originalArtifact" );

        // set in the past to ensure it is downloaded
        localRepoFile.setLastModified( System.currentTimeMillis() - TIME_OFFSET );

        FileUtils.fileWrite( artifact.getAbsolutePath(), "updatedArtifact" );

        verifier.executeGoal( "package" );

        verifyArtifactContent( "updatedArtifact" );

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSnapshotUpdatedWithMetadata()
        throws Exception
    {
        File metadata =
            new File( repository, "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml" );
        FileUtils.fileWrite( metadata.getAbsolutePath(),
                             constructMetadata( "1", System.currentTimeMillis() - TIME_OFFSET, true ) );

        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();

        verifyArtifactContent( "originalArtifact" );

        FileUtils.fileWrite( artifact.getAbsolutePath(), "updatedArtifact" );
        metadata = new File( repository, "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml" );
        FileUtils.fileWrite( metadata.getAbsolutePath(), constructMetadata( "2", System.currentTimeMillis(), true ) );

        verifier.executeGoal( "package" );

        verifyArtifactContent( "updatedArtifact" );

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSnapshotUpdatedWithLocalMetadata()
        throws Exception
    {
        File localMetadata = getMetadataFile( "org/apache/maven", "maven-core-it-support", "1.0-SNAPSHOT" );

        FileUtils.deleteDirectory( localMetadata.getParentFile() );
        assertFalse( localMetadata.getParentFile().exists() );
        localMetadata.getParentFile().mkdirs();

        File metadata =
            new File( repository, "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml" );
        FileUtils.fileWrite( metadata.getAbsolutePath(),
                             constructMetadata( "1", System.currentTimeMillis() - TIME_OFFSET, true ) );

        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();

        verifyArtifactContent( "originalArtifact" );
        assertFalse( localMetadata.exists() );

        FileUtils.fileWrite( localRepoFile.getAbsolutePath(), "localArtifact" );
        FileUtils.fileWrite( localMetadata.getAbsolutePath(), constructLocalMetadata( "org.apache.maven",
                                                                                      "maven-core-it-support",
                                                                                      System.currentTimeMillis(),
                                                                                      true ) );
        // update the remote file, but we shouldn't be looking
        artifact.setLastModified( System.currentTimeMillis() );

        verifier.executeGoal( "package" );

        verifyArtifactContent( "localArtifact" );

        verifier.verifyErrorFreeLog();

        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.YEAR, -1 );
        FileUtils.fileWrite( localMetadata.getAbsolutePath(), constructLocalMetadata( "org.apache.maven",
                                                                                      "maven-core-it-support",
                                                                                      cal.getTimeInMillis(), true ) );
        FileUtils.fileWrite( metadata.getAbsolutePath(),
                             constructMetadata( "2", System.currentTimeMillis() - 2000, true ) );
        artifact.setLastModified( System.currentTimeMillis() );

        verifier.executeGoal( "package" );

        verifyArtifactContent( "originalArtifact" );

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSnapshotUpdatedWithMetadataUsingFileTimestamp()
        throws Exception
    {
        File metadata =
            new File( repository, "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml" );
        FileUtils.fileWrite( metadata.getAbsolutePath(),
                             constructMetadata( "1", System.currentTimeMillis() - TIME_OFFSET, false ) );
        metadata.setLastModified( System.currentTimeMillis() - TIME_OFFSET );

        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();

        verifyArtifactContent( "originalArtifact" );

        FileUtils.fileWrite( artifact.getAbsolutePath(), "updatedArtifact" );
        metadata = new File( repository, "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml" );
        FileUtils.fileWrite( metadata.getAbsolutePath(), constructMetadata( "2", System.currentTimeMillis(), false ) );

        verifier.executeGoal( "package" );

        verifyArtifactContent( "updatedArtifact" );

        verifier.verifyErrorFreeLog();
    }

    private File getMetadataFile( String groupId, String artifactId, String version )
    {
        return new File( verifier.getArtifactMetadataPath( groupId, artifactId, version, "maven-metadata-local.xml" ) );
    }

    private void verifyArtifactContent( String s )
        throws IOException, VerificationException
    {
        verifier.verifyArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar" );
        verifier.verifyArtifactContent( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar", s );
    }

    private static File deleteLocalArtifact( Verifier verifier, File localRepoFile )
        throws IOException
    {
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar" );
        // this is to delete metadata - TODO: incorporate into deleteArtifact in verifier
        FileUtils.deleteDirectory( localRepoFile.getParentFile() );
        return localRepoFile;
    }

    private static File getLocalRepoFile( Verifier verifier )
    {
        return new File(
            verifier.getArtifactPath( "org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar" ) );
    }

    private static void recreateRemoteRepository( File repository )
        throws IOException
    {
        // create a repository (TODO: into verifier)
        FileUtils.deleteDirectory( repository );
        assertFalse( repository.exists() );
        repository.mkdirs();
    }

    private String constructMetadata( String buildNumber, long timestamp, boolean writeLastUpdated )
    {
        String ts = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US ).format( new Date( timestamp ) );

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata>\n" + "<groupId>org.apache.maven</groupId>\n" +
            "<artifactId>maven-core-it-support</artifactId>\n" + "<version>1.0-SNAPSHOT</version>\n" +
            "<versioning>\n" + "<snapshot>\n" + "<buildNumber>" + buildNumber + "</buildNumber>\n" + "</snapshot>\n" +
            ( writeLastUpdated ? "<lastUpdated>" + ts + "</lastUpdated>\n" : "" ) + "</versioning>\n" + "</metadata>";
    }

    private String constructLocalMetadata( String groupId, String artifactId, long timestamp, boolean writeLastUpdated )
    {
        String ts = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US ).format( new Date( timestamp ) );

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata>\n" + "  <groupId>" + groupId + "</groupId>\n" +
            "  <artifactId>" + artifactId + "</artifactId>\n" + "  <version>1.0-SNAPSHOT</version>\n" +
            "  <versioning>\n" + "    <snapshot>\n" + "      <localCopy>true</localCopy>\n" + "    </snapshot>\n" +
            ( writeLastUpdated ? "    <lastUpdated>" + ts + "</lastUpdated>\n" : "" ) + "  </versioning>\n" +
            "</metadata>";
    }
}
