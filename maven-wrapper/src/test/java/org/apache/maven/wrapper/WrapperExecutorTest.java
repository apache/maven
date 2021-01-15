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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class WrapperExecutorTest
{
    @TempDir
    public Path testDir;

    private Installer install;

    private BootstrapMainStarter start;

    private Path propertiesFile;

    private Properties properties = new Properties();

    private Path mockInstallDir;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        mockInstallDir = testDir.resolve( "mock-dir" );

        install = mock( Installer.class );
        when( install.createDist( Mockito.any( WrapperConfiguration.class ) ) ).thenReturn( mockInstallDir );
        start = mock( BootstrapMainStarter.class );

        propertiesFile = testDir.resolve( "maven" ).resolve( "wrapper" ).resolve( "maven-wrapper.properties" );

        properties.put( "distributionUrl", "http://server/test/maven.zip" );
        properties.put( "distributionBase", "testDistBase" );
        properties.put( "distributionPath", "testDistPath" );
        properties.put( "zipStoreBase", "testZipBase" );
        properties.put( "zipStorePath", "testZipPath" );

        writePropertiesFile( properties, propertiesFile, "header" );
    }

    @Test
    public void loadWrapperMetadataFromFile()
        throws Exception
    {
        WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );

        assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getDistribution() );
        assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getConfiguration().getDistribution() );
        assertEquals( "testDistBase", wrapper.getConfiguration().getDistributionBase() );
        assertEquals( "testDistPath", wrapper.getConfiguration().getDistributionPath() );
        assertEquals( "testZipBase", wrapper.getConfiguration().getZipBase() );
        assertEquals( "testZipPath", wrapper.getConfiguration().getZipPath() );
    }

    @Test
    public void loadWrapperMetadataFromDirectory()
        throws Exception
    {
        WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory( testDir );

        assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getDistribution() );
        assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getConfiguration().getDistribution() );
        assertEquals( "testDistBase", wrapper.getConfiguration().getDistributionBase() );
        assertEquals( "testDistPath", wrapper.getConfiguration().getDistributionPath() );
        assertEquals( "testZipBase", wrapper.getConfiguration().getZipBase() );
        assertEquals( "testZipPath", wrapper.getConfiguration().getZipPath() );
    }

    @Test
    public void useDefaultMetadataNoProeprtiesFile()
        throws Exception
    {
        WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory( testDir.resolve( "unknown" ) );

        assertNull( wrapper.getDistribution() );
        assertNull( wrapper.getConfiguration().getDistribution() );
        assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase() );
        assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath() );
        assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getZipBase() );
        assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath() );
    }

    @Test
    public void propertiesFileOnlyContainsDistURL()
        throws Exception
    {

        properties = new Properties();
        properties.put( "distributionUrl", "http://server/test/maven.zip" );
        writePropertiesFile( properties, propertiesFile, "header" );

        WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );

        assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getDistribution() );
        assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getConfiguration().getDistribution() );
        assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase() );
        assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath() );
        assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getZipBase() );
        assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath() );
    }

    @Test
    public void executeInstallAndLaunch()
        throws Exception
    {
        WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory( propertiesFile );

        wrapper.execute( new String[] { "arg" }, install, start );
        verify( install ).createDist( Mockito.any( WrapperConfiguration.class ) );
        verify( start ).start( new String[] { "arg" }, mockInstallDir );
    }

    @Test( )
    public void failWhenDistNotSetInProperties()
        throws Exception
    {
        properties = new Properties();
        writePropertiesFile( properties, propertiesFile, "header" );

        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> WrapperExecutor.forWrapperPropertiesFile( propertiesFile ),
                "Expected RuntimeException" );
        assertEquals( "No value with key 'distributionUrl' specified in wrapper properties file '"
            + propertiesFile + "'.", e.getMessage() );
    }

    @Test
    public void failWhenPropertiesFileDoesNotExist()
    {
        propertiesFile = testDir.resolve( "unknown.properties" );

        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> WrapperExecutor.forWrapperPropertiesFile( propertiesFile ),
                "Expected RuntimeException" );
        assertEquals( "Wrapper properties file '" + propertiesFile + "' does not exist.", e.getMessage() );
    }

    @Test
    public void testRelativeDistUrl()
        throws Exception
    {

        properties = new Properties();
        properties.put( "distributionUrl", "some/relative/url/to/bin.zip" );
        writePropertiesFile( properties, propertiesFile, "header" );

        WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );
        assertNotEquals( "some/relative/url/to/bin.zip", wrapper.getDistribution().getSchemeSpecificPart() );
        assertTrue( wrapper.getDistribution().getSchemeSpecificPart().endsWith( "some/relative/url/to/bin.zip" ) );
    }

    private void writePropertiesFile( Properties properties, Path propertiesFile, String message )
        throws IOException
    {
        Files.createDirectories( propertiesFile.getParent() );

        try ( OutputStream outStream = Files.newOutputStream( propertiesFile ) )
        {
            properties.store( outStream, message );
        }
    }
}
