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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

public class MavenITmng6754TimestampInMultimoduleProject
        extends AbstractMavenIntegrationTestCase
{
    private static final String RESOURCE_PATH = "/mng-6754-version-timestamp-in-multimodule-build";
    private static final String VERSION = "1.0-SNAPSHOT";


    public MavenITmng6754TimestampInMultimoduleProject()
    {
        super( "[3.8.2,)" );
    }

    @Test
    public void testArtifactsHaveSameTimestamp()
            throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_PATH );
        final Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        final Path localRepoDir = Paths.get( verifier.getLocalRepository() );
        final Path remoteRepoDir = Paths.get( verifier.getBasedir(), "repo" );

        verifier.deleteDirectory( "repo" );
        verifier.deleteArtifacts ( "org.apache.maven.its.mng6754" );
        verifier.addCliArgument( "-Drepodir=" + remoteRepoDir );
        verifier.addCliArgument( "deploy" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        final Properties props = verifier.loadProperties( "target/timestamp.properties" );
        // Reference timestamp
        final String mavenBuildTimestamp = props.getProperty( "project.properties.timestamp" );

        final Metadata parentMetadataLocal = getMetadata( getLocalMetadataPath( localRepoDir, "parent", null ) );
        final Metadata aMetadataLocal = getMetadata( getLocalMetadataPath( localRepoDir, "child-a", null ) );
        final Metadata bMetadataLocal = getMetadata( getLocalMetadataPath( localRepoDir, "child-b", null ) );

        final String parentLastUpdatedLocal = parentMetadataLocal.getVersioning().getLastUpdated();
        final String aLastUpdatedLocal = aMetadataLocal.getVersioning().getLastUpdated();
        final String bLastUpdatedLocal = bMetadataLocal.getVersioning().getLastUpdated();

        assertEquals ( "parent", "local", "lastUpdated", mavenBuildTimestamp, parentLastUpdatedLocal );
        assertEquals ( "child-a", "local", "lastUpdated", mavenBuildTimestamp, aLastUpdatedLocal );
        assertEquals ( "child-b", "local", "lastUpdated", mavenBuildTimestamp, bLastUpdatedLocal );

        final Metadata parentVersionedMetadataLocal = getMetadata( getLocalMetadataPath( localRepoDir, "parent", VERSION ) );
        final Metadata aVersionedMetadataLocal = getMetadata( getLocalMetadataPath( localRepoDir, "child-a", VERSION ) );
        final Metadata bVersionedMetadataLocal = getMetadata( getLocalMetadataPath( localRepoDir, "child-b", VERSION ) );

        final String parentVersionedLastUpdatedLocal = parentVersionedMetadataLocal.getVersioning().getLastUpdated();
        final String parentVersionedSnapshotVersionUpdatedLocal = parentVersionedMetadataLocal.getVersioning().getSnapshotVersions().get( 0 ).getUpdated();
        final String aLastVersionedUpdatedLocal = aVersionedMetadataLocal.getVersioning().getLastUpdated();
        final String aVersionedSnapshotVersionUpdated1Local = aVersionedMetadataLocal.getVersioning().getSnapshotVersions().get( 0 ).getUpdated();
        final String aVersionedSnapshotVersionUpdated2Local = aVersionedMetadataLocal.getVersioning().getSnapshotVersions().get( 1 ).getUpdated();
        final String bLastVersionedUpdatedLocal = bVersionedMetadataLocal.getVersioning().getLastUpdated();
        final String bVersionedSnapshotVersionUpdated1Local = bVersionedMetadataLocal.getVersioning().getSnapshotVersions().get( 0 ).getUpdated();
        final String bVersionedSnapshotVersionUpdated2Local = bVersionedMetadataLocal.getVersioning().getSnapshotVersions().get( 1 ).getUpdated();

        assertEquals ( "parent", "local", "lastUpdated", mavenBuildTimestamp, parentVersionedLastUpdatedLocal );
        assertEquals ( "parent", "local", "snapshotVersion[0]/updated", mavenBuildTimestamp, parentVersionedSnapshotVersionUpdatedLocal );
        assertEquals ( "child-a", "local", "lastUpdated", mavenBuildTimestamp, aLastVersionedUpdatedLocal );
        assertEquals ( "child-a", "local", "snapshotVersion[0]/updated", mavenBuildTimestamp, aVersionedSnapshotVersionUpdated1Local );
        assertEquals ( "child-a", "local", "snapshotVersion[1]/updated", mavenBuildTimestamp, aVersionedSnapshotVersionUpdated2Local );
        assertEquals ( "child-b", "local", "lastUpdated", mavenBuildTimestamp, bLastVersionedUpdatedLocal );
        assertEquals ( "child-b", "local", "snapshotVersion[0]/updated", mavenBuildTimestamp, bVersionedSnapshotVersionUpdated1Local );
        assertEquals ( "child-b", "local", "snapshotVersion[1]/updated", mavenBuildTimestamp, bVersionedSnapshotVersionUpdated2Local );

        final Metadata parentMetadataRemote = getMetadata( getRemoteMetadataPath( remoteRepoDir, "parent", null ) );
        final Metadata aMetadataRemote = getMetadata( getRemoteMetadataPath( remoteRepoDir, "child-a", null ) );
        final Metadata bMetadataRemote = getMetadata( getRemoteMetadataPath( remoteRepoDir, "child-b", null ) );

        final String parentLastUpdatedRemote = parentMetadataRemote.getVersioning().getLastUpdated();
        final String aLastUpdatedRemote = aMetadataRemote.getVersioning().getLastUpdated();
        final String bLastUpdatedRemote = bMetadataRemote.getVersioning().getLastUpdated();

        assertEquals ( "parent", "remote", "lastUpdated", mavenBuildTimestamp, parentLastUpdatedRemote );
        assertEquals ( "child-a", "remote", "lastUpdated", mavenBuildTimestamp, aLastUpdatedRemote );
        assertEquals ( "child-b", "remote", "lastUpdated", mavenBuildTimestamp, bLastUpdatedRemote );

        final Metadata parentVersionedMetadataRemote = getMetadata( getRemoteMetadataPath( remoteRepoDir, "parent", VERSION ) );
        final Metadata aVersionedMetadataRemote = getMetadata( getRemoteMetadataPath( remoteRepoDir, "child-a", VERSION ) );
        final Metadata bVersionedMetadataRemote = getMetadata( getRemoteMetadataPath( remoteRepoDir, "child-b", VERSION ) );

        final String parentVersionedLastUpdatedRemote = parentVersionedMetadataRemote.getVersioning().getLastUpdated();
        final String parentVersionedSnapshotTimestamp = parentVersionedMetadataRemote.getVersioning().getSnapshot().getTimestamp().replace( ".", "" );
        final String parentVersionedSnapshotVersionUpdatedRemote = parentVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 0 ).getUpdated();
        final String parentVersionedSnapshotVersionValueRemote = parentVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 0 ).getVersion();
        final String aLastVersionedUpdatedRemote = aVersionedMetadataRemote.getVersioning().getLastUpdated();
        final String aVersionedSnapshotTimestamp = aVersionedMetadataRemote.getVersioning().getSnapshot().getTimestamp().replace( ".", "" );
        final String aVersionedSnapshotVersionUpdated1Remote = aVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 0 ).getUpdated();
        final String aVersionedSnapshotVersionValue1Remote = aVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 0 ).getVersion();
        final String aVersionedSnapshotVersionUpdated2Remote = aVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 1 ).getUpdated();
        final String aVersionedSnapshotVersionValue2Remote = aVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 1 ).getVersion();
        final String bLastVersionedUpdatedRemote = bVersionedMetadataRemote.getVersioning().getLastUpdated();
        final String bVersionedSnapshotTimestamp = bVersionedMetadataRemote.getVersioning().getSnapshot().getTimestamp().replace( ".", "" );
        final String bVersionedSnapshotVersionUpdated1Remote = bVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 0 ).getUpdated();
        final String bVersionedSnapshotVersionValue1Remote = bVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 0 ).getVersion();
        final String bVersionedSnapshotVersionUpdated2Remote = bVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 1 ).getUpdated();
        final String bVersionedSnapshotVersionValue2Remote = bVersionedMetadataRemote.getVersioning().getSnapshotVersions().get( 1 ).getVersion();

        assertEquals ( "parent", "remote", "lastUpdated", mavenBuildTimestamp, parentVersionedLastUpdatedRemote );
        assertEquals ( "parent", "remote", "snapshot/timestamp", mavenBuildTimestamp, parentVersionedSnapshotTimestamp );
        assertEquals ( "parent", "remote", "snapshotVersion[0]/updated", mavenBuildTimestamp, parentVersionedSnapshotVersionUpdatedRemote );
        assertEquals ( "parent", "remote", "snapshotVersion[0]/value", mavenBuildTimestamp, parentVersionedSnapshotVersionValueRemote.substring( 4, 19 ).replace( ".", "" ) );
        assertEquals ( "child-a", "remote", "lastUpdated", mavenBuildTimestamp, aLastVersionedUpdatedRemote );
        assertEquals ( "child-a", "remote", "snapshot/timestamp", mavenBuildTimestamp, aVersionedSnapshotTimestamp );
        assertEquals ( "child-a", "remote", "snapshotVersion[0]/updated", mavenBuildTimestamp, aVersionedSnapshotVersionUpdated1Remote );
        assertEquals ( "child-a", "remote", "snapshotVersion[0]/value", mavenBuildTimestamp, aVersionedSnapshotVersionValue1Remote.substring( 4, 19 ).replace( ".", "" ) );
        assertEquals ( "child-a", "remote", "snapshotVersion[1]/updated", mavenBuildTimestamp, aVersionedSnapshotVersionUpdated2Remote );
        assertEquals ( "child-a", "remote", "snapshotVersion[1]/value", mavenBuildTimestamp, aVersionedSnapshotVersionValue2Remote.substring( 4, 19 ).replace( ".", "" ) );
        assertEquals ( "child-b", "remote", "lastUpdated", mavenBuildTimestamp, bLastVersionedUpdatedRemote );
        assertEquals ( "child-b", "remote", "snapshot/timestamp", mavenBuildTimestamp, bVersionedSnapshotTimestamp );
        assertEquals ( "child-b", "remote", "snapshotVersion[0]/updated", mavenBuildTimestamp, bVersionedSnapshotVersionUpdated1Remote );
        assertEquals ( "child-b", "remote", "snapshotVersion[0]/value", mavenBuildTimestamp, bVersionedSnapshotVersionValue1Remote.substring( 4, 19 ).replace( ".", "" ) );
        assertEquals ( "child-b", "remote", "snapshotVersion[1]/updated", mavenBuildTimestamp, bVersionedSnapshotVersionUpdated2Remote );
        assertEquals ( "child-b", "remote", "snapshotVersion[1]/value", mavenBuildTimestamp, bVersionedSnapshotVersionValue2Remote.substring( 4, 19 ).replace( ".", "" ) );
        assertPathExists( remoteRepoDir, "parent", "remote", VERSION, "parent-" + parentVersionedSnapshotVersionValueRemote + ".pom" );
        assertPathExists( remoteRepoDir, "child-a", "remote", VERSION, "child-a-" + aVersionedSnapshotVersionValue1Remote + ".pom" );
        assertPathExists( remoteRepoDir, "child-a", "remote", VERSION, "child-a-" + aVersionedSnapshotVersionValue2Remote + ".jar" );
        assertPathExists( remoteRepoDir, "child-b", "remote", VERSION, "child-b-" + bVersionedSnapshotVersionValue1Remote + ".pom" );
        assertPathExists( remoteRepoDir, "child-b", "remote", VERSION, "child-b-" + bVersionedSnapshotVersionValue2Remote + ".jar" );
    }

    private Path getLocalMetadataPath( final Path repoDir, final String moduleName, String version )
    {
        return getRepoFile(repoDir, moduleName, version, "maven-metadata-local.xml" );
    }

    private Path getRemoteMetadataPath( final Path repoDir, final String moduleName, String version )
    {
        return getRepoFile(repoDir, moduleName, version, "maven-metadata.xml" );
    }

    private Path getRepoFile( final Path repoDir, final String moduleName, String version, String fileName )
    {
        final Path mng6754Path = Paths.get( "org", "apache", "maven", "its", "mng6754" );
        Path modulePath = repoDir.resolve( mng6754Path.resolve( moduleName ) );
        if ( version != null )
        {
            modulePath = modulePath.resolve( version );
        }
        return modulePath.resolve( fileName );
    }

    private Metadata getMetadata( final Path metadataFile ) throws IOException, XmlPullParserException
    {
        MetadataXpp3Reader r = new MetadataXpp3Reader();
        try ( InputStream is = Files.newInputStream( metadataFile ) )
        {
            return r.read( is );
        }
    }

    private void assertEquals( String moduleName, String location, String field, String expected, String actual )
    {
        String phase = null;
        switch ( location )
        {
        case "local":
            phase = "Installed";
            break;
        case "remote":
            phase = "Deployed";
            break;
        }
        assertEquals( String.format( "%s %s module should have equal %s %s with the Maven build timestamp",
                 phase, moduleName, location, field ), expected, actual );
    }

    private void assertPathExists( Path repoDir, String moduleName, String location, String version, String fileName )
    {
        String phase = null;
        switch ( location )
        {
        case "local":
            phase = "Installed";
            break;
        case "remote":
            phase = "Deployed";
            break;
        }
        Path file = getRepoFile( repoDir, moduleName, version, fileName );
        assertTrue( String.format( "%s %s module %s file %s should exist",
                 phase, moduleName, location, file ), Files.exists( file ) );
    }
}
