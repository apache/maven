package org.apache.maven.toolchain.building;

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

import org.apache.maven.building.StringSource;
import org.apache.maven.toolchain.io.DefaultToolchainsReader;
import org.apache.maven.toolchain.io.DefaultToolchainsWriter;
import org.apache.maven.toolchain.io.ToolchainsParseException;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.interpolation.os.OperatingSystemUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class DefaultToolchainsBuilderTest
{
    private static final String LS = System.lineSeparator();

    @Spy
    private DefaultToolchainsReader toolchainsReader;

    @Spy
    private DefaultToolchainsWriter toolchainsWriter;

    @InjectMocks
    private DefaultToolchainsBuilder toolchainBuilder;

    @BeforeEach
    public void onSetup()
    {
        MockitoAnnotations.initMocks( this );

        Map<String, String> envVarMap = new HashMap<>();
        envVarMap.put("testKey", "testValue");
        envVarMap.put("testSpecialCharactersKey", "<test&Value>");
        OperatingSystemUtils.setEnvVarSource(new TestEnvVarSource(envVarMap));
    }

    @Test
    public void testBuildEmptyRequest()
        throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        assertNotNull( result.getEffectiveToolchains() );
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    @Test
    public void testBuildRequestWithUserToolchains()
        throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource( new StringSource( "" ) );

        PersistedToolchains userResult = new PersistedToolchains();
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType( "TYPE" );
        toolchain.addProvide( "key", "user_value" );
        userResult.addToolchain(  toolchain );
        doReturn(userResult).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        assertNotNull( result.getEffectiveToolchains() );
        assertEquals( 1, result.getEffectiveToolchains().getToolchains().size() );
        assertEquals( "TYPE", result.getEffectiveToolchains().getToolchains().get(0).getType() );
        assertEquals( "user_value", result.getEffectiveToolchains().getToolchains().get(0).getProvides().getProperty( "key" ) );
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    @Test
    public void testBuildRequestWithGlobalToolchains()
        throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource( new StringSource( "" ) );

        PersistedToolchains globalResult = new PersistedToolchains();
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType( "TYPE" );
        toolchain.addProvide( "key", "global_value" );
        globalResult.addToolchain(  toolchain );
        doReturn(globalResult).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        assertNotNull( result.getEffectiveToolchains() );
        assertEquals( 1, result.getEffectiveToolchains().getToolchains().size() );
        assertEquals( "TYPE", result.getEffectiveToolchains().getToolchains().get(0).getType() );
        assertEquals( "global_value", result.getEffectiveToolchains().getToolchains().get(0).getProvides().getProperty( "key" ) );
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    @Test
    public void testBuildRequestWithBothToolchains()
        throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource( new StringSource( "" ) );
        request.setUserToolchainsSource( new StringSource( "" ) );

        PersistedToolchains userResult = new PersistedToolchains();
        ToolchainModel userToolchain = new ToolchainModel();
        userToolchain.setType( "TYPE" );
        userToolchain.addProvide( "key", "user_value" );
        userResult.addToolchain(  userToolchain );

        PersistedToolchains globalResult = new PersistedToolchains();
        ToolchainModel globalToolchain = new ToolchainModel();
        globalToolchain.setType( "TYPE" );
        globalToolchain.addProvide( "key", "global_value" );
        globalResult.addToolchain(  globalToolchain );
        doReturn(globalResult).doReturn(userResult).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        assertNotNull( result.getEffectiveToolchains() );
        assertEquals( 2, result.getEffectiveToolchains().getToolchains().size() );
        assertEquals( "TYPE", result.getEffectiveToolchains().getToolchains().get(0).getType() );
        assertEquals( "user_value", result.getEffectiveToolchains().getToolchains().get(0).getProvides().getProperty( "key" ) );
        assertEquals( "TYPE", result.getEffectiveToolchains().getToolchains().get(1).getType() );
        assertEquals( "global_value", result.getEffectiveToolchains().getToolchains().get(1).getProvides().getProperty( "key" ) );
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    @Test
    public void testStrictToolchainsParseException() throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource( new StringSource( "" ) );
        ToolchainsParseException parseException = new ToolchainsParseException( "MESSAGE", 4, 2 );
        doThrow(parseException).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        try
        {
            toolchainBuilder.build( request );
        }
        catch ( ToolchainsBuildingException e )
        {
            assertEquals( "1 problem was encountered while building the effective toolchains" + LS +
                "[FATAL] Non-parseable toolchains (memory): MESSAGE @ line 4, column 2" + LS, e.getMessage() );
        }
    }

    @Test
    public void testIOException() throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource( new StringSource( "", "LOCATION" ) );
        IOException ioException = new IOException( "MESSAGE" );
        doThrow(ioException).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        try
        {
            toolchainBuilder.build( request );
        }
        catch ( ToolchainsBuildingException e )
        {
            assertEquals( "1 problem was encountered while building the effective toolchains" + LS +
                "[FATAL] Non-readable toolchains LOCATION: MESSAGE" + LS, e.getMessage() );
        }
    }

    @Test
    public void testEnvironmentVariablesAreInterpolated()
            throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource( new StringSource( "" ) );

        PersistedToolchains persistedToolchains = new PersistedToolchains();
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType( "TYPE" );
        toolchain.addProvide( "key", "${env.testKey}" );

        Xpp3Dom configurationChild = new Xpp3Dom("jdkHome");
        configurationChild.setValue("${env.testKey}");
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(configurationChild);
        toolchain.setConfiguration(configuration);
        persistedToolchains.addToolchain( toolchain );
        doReturn(persistedToolchains).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        String interpolatedValue = "testValue";
        assertEquals(interpolatedValue, result.getEffectiveToolchains().getToolchains().get(0).getProvides().getProperty( "key" ) );
        Xpp3Dom toolchainConfiguration = (Xpp3Dom) result.getEffectiveToolchains().getToolchains().get(0).getConfiguration();
        assertEquals(interpolatedValue, toolchainConfiguration.getChild("jdkHome").getValue());
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    @Test
    public void testNonExistingEnvironmentVariablesAreNotInterpolated()
            throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource( new StringSource( "" ) );

        PersistedToolchains persistedToolchains = new PersistedToolchains();
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType( "TYPE" );
        toolchain.addProvide( "key", "${env.testNonExistingKey}" );

        persistedToolchains.addToolchain( toolchain );
        doReturn(persistedToolchains).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        assertEquals("${env.testNonExistingKey}", result.getEffectiveToolchains().getToolchains().get(0).getProvides().getProperty( "key" ) );
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    @Test
    public void testEnvironmentVariablesWithSpecialCharactersAreInterpolated()
            throws Exception
    {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource( new StringSource( "" ) );

        PersistedToolchains persistedToolchains = new PersistedToolchains();
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType( "TYPE" );
        toolchain.addProvide( "key", "${env.testSpecialCharactersKey}" );

        persistedToolchains.addToolchain( toolchain );
        doReturn(persistedToolchains).when( toolchainsReader ).read( any( InputStream.class ), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build( request );
        String interpolatedValue = "<test&Value>";
        assertEquals(interpolatedValue, result.getEffectiveToolchains().getToolchains().get(0).getProvides().getProperty( "key" ) );
        assertNotNull( result.getProblems() );
        assertEquals( 0, result.getProblems().size() );
    }

    static class TestEnvVarSource implements OperatingSystemUtils.EnvVarSource {
        private final Map<String, String> envVarMap;

        TestEnvVarSource(Map<String, String> envVarMap) {
            this.envVarMap = envVarMap;
        }

        public Map<String, String> getEnvMap() {
            return envVarMap;
        }
    }

}
