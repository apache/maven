package org.apache.maven.wrapper;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Hans Dockter
 */
public class InstallerTest
{
    @TempDir
    public Path temporaryFolder;

    private Installer install;

    private Path distributionDir;

    private Path zipStore;

    private Path mavenHomeDir;

    private Path zipDestination;

    private WrapperConfiguration configuration = new WrapperConfiguration();

    private Downloader download;

    private PathAssembler pathAssembler;

    private PathAssembler.LocalDistribution localDistribution;

    @BeforeEach
    public void setup()
        throws Exception
    {
        configuration.setZipBase( PathAssembler.PROJECT_STRING );
        configuration.setZipPath( "someZipPath" );
        configuration.setDistributionBase( PathAssembler.MAVEN_USER_HOME_STRING );
        configuration.setDistributionPath( "someDistPath" );
        configuration.setDistribution( new URI( "http://server/maven-0.9.zip" ) );
        configuration.setAlwaysDownload( false );
        configuration.setAlwaysUnpack( false );
        distributionDir = temporaryFolder.resolve( "someDistPath" );
        Files.createDirectories( distributionDir );
        mavenHomeDir = distributionDir.resolve( "maven-0.9" );
        zipStore = temporaryFolder.resolve( "zips" );
        Files.createDirectories( zipStore );
        zipDestination = zipStore.resolve( "maven-0.9.zip" );

        download = mock( Downloader.class );
        pathAssembler = mock( PathAssembler.class );
        localDistribution = mock( PathAssembler.LocalDistribution.class );

        when( localDistribution.getZipFile() ).thenReturn( zipDestination );
        when( localDistribution.getDistributionDir() ).thenReturn( distributionDir );
        when( pathAssembler.getDistribution( configuration ) ).thenReturn( localDistribution );

        install = new Installer( download, pathAssembler );
    }

    private void createTestZip( Path zipDestination )
        throws Exception
    {
        Files.createDirectories( zipDestination.getParent() );

        Path explodedZipDir = temporaryFolder.resolve( "explodedZip" );

        Path mavenScript = explodedZipDir.resolve( "maven-0.9/bin/mvn" );
        Files.createDirectories( mavenScript.getParent() );
        Files.write( mavenScript, Arrays.asList( "something" ) );

        zipTo( explodedZipDir, zipDestination );
    }

    @Test
    @Disabled("not working")
    public void testCreateDist()
        throws Exception
    {
        Path homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( Files.isDirectory( homeDir ) );
        Assert.assertTrue( Files.exists( homeDir.resolve( "bin/mvn" ) ) );
        Assert.assertTrue( Files.exists( zipDestination ) );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );

        // download.download(new URI("http://some/test"), distributionDir);
        // verify(download).download(new URI("http://some/test"), distributionDir);
    }

    @Test
    public void testCreateDistWithExistingDistribution()
        throws Exception
    {
        Files.createFile( zipDestination );

        Files.createDirectories( mavenHomeDir );
        Path someFile = mavenHomeDir.resolve( "some-file" );
        Files.createFile( someFile );

        Path homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( Files.isDirectory( mavenHomeDir ) );
        Assert.assertTrue( Files.exists( homeDir.resolve( "some-file" ) ) );
        Assert.assertTrue( Files.exists( zipDestination ) );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );
    }

    @Test
    public void testCreateDistWithExistingDistAndZipAndAlwaysUnpackTrue()
        throws Exception
    {

        createTestZip( zipDestination );
        Files.createDirectories( mavenHomeDir );
        File garbage = mavenHomeDir.resolve( "garbage" ).toFile();
        Files.createFile( garbage.toPath() );

        configuration.setAlwaysUnpack( true );

        Path homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( Files.isDirectory( mavenHomeDir ) );
        Assert.assertFalse( Files.exists( homeDir.resolve( "garbage" ) ) );
        Assert.assertTrue( Files.exists( zipDestination ) );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );
    }

    @Test
    public void testCreateDistWithExistingZipAndDistAndAlwaysDownloadTrue()
        throws Exception
    {

        createTestZip( zipDestination );
        Files.createDirectories( mavenHomeDir );
        File garbage = mavenHomeDir.resolve( "garbage" ).toFile();
        Files.createFile( garbage.toPath() );

        configuration.setAlwaysUnpack( true );

        Path homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( Files.isDirectory( mavenHomeDir ) );
        Assert.assertTrue( Files.exists( homeDir.resolve( "bin/mvn" ) ) );
        Assert.assertFalse( Files.exists( homeDir.resolve( "garbage" ) ) );
        Assert.assertTrue( Files.exists( zipDestination ) );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );

        // download.download(new URI("http://some/test"), distributionDir);
        // verify(download).download(new URI("http://some/test"), distributionDir);
    }

    public void zipTo( Path directoryToZip, Path zipFile )
    {
        Zip zip = new Zip();
        zip.setBasedir( directoryToZip.toFile() );
        zip.setDestFile( zipFile.toFile() );
        zip.setProject( new Project() );

        Zip.WhenEmpty whenEmpty = new Zip.WhenEmpty();
        whenEmpty.setValue( "create" );
        zip.setWhenempty( whenEmpty );
        zip.execute();
    }
}
