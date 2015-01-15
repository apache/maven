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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.building.StringSource;
import org.apache.maven.toolchain.io.ToolchainsParseException;
import org.apache.maven.toolchain.io.ToolchainsReader;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultToolchainsBuilderTest
{
    private static final String LS = System.getProperty( "line.separator" );
    
    @Mock
    private ToolchainsReader toolchainsReader;

    @InjectMocks
    private DefaultToolchainsBuilder toolchainBuilder = new DefaultToolchainsBuilder();

    @Before
    public void onSetup()
    {
        MockitoAnnotations.initMocks( this );
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
        when( toolchainsReader.read( any( InputStream.class ), anyMap() ) ).thenReturn( userResult );

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
        when( toolchainsReader.read( any( InputStream.class ), anyMap() ) ).thenReturn( globalResult );

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
        when( toolchainsReader.read( any( InputStream.class ), anyMap() ) ).thenReturn( globalResult ).thenReturn( userResult );

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
        when( toolchainsReader.read( any( InputStream.class ), anyMap() ) ).thenThrow( parseException );
        
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
        when( toolchainsReader.read( any( InputStream.class ), anyMap() ) ).thenThrow( ioException );
        
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
    
}
