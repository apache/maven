package org.apache.maven.toolchain;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingException;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultToolchainManagerPrivateTest
{
    // Mocks to inject into toolchainManager
    @Mock
    private org.apache.maven.toolchain.building.ToolchainsBuilder toolchainsBuilder;

    @Mock
    private Logger logger;

    @InjectMocks
    private DefaultToolchainManagerPrivate toolchainManager;

    @Mock
    private ToolchainFactory toolchainFactory_basicType;
    
    @Mock
    private ToolchainFactory toolchainFactory_rareType;

    @Before
    public void setUp()
    {
        toolchainManager = new DefaultToolchainManagerPrivate();

        MockitoAnnotations.initMocks( this );

        toolchainManager.factories = new HashMap<String, ToolchainFactory>();
        toolchainManager.factories.put( "basic", toolchainFactory_basicType );
        toolchainManager.factories.put( "rare", toolchainFactory_rareType );
    }

    @Test
    public void testToolchainsForAvailableType()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when( session.getRequest() ).thenReturn( req );

        ToolchainsBuildingResult toolchainsResult = new DefaultToolchainsBuildingResult( new PersistedToolchains(), null );
        when( toolchainsBuilder.build( isA( ToolchainsBuildingRequest.class ) ) ).thenReturn( toolchainsResult );
        ToolchainPrivate basicToolchain = mock( ToolchainPrivate.class );
        when( toolchainFactory_basicType.createDefaultToolchain() ).thenReturn( basicToolchain );
        ToolchainPrivate rareToolchain = mock( ToolchainPrivate.class );
        when( toolchainFactory_rareType.createDefaultToolchain() ).thenReturn( rareToolchain );

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType( "basic", session );

        // verify
        verify( logger, never() ).error( anyString() );
        assertEquals( 1, toolchains.length );
    }

    @Test
    public void testToolchainsForUnknownType()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when( session.getRequest() ).thenReturn( req );

        ToolchainsBuildingResult toolchainsResult = new DefaultToolchainsBuildingResult( new PersistedToolchains(), null );
        when( toolchainsBuilder.build( isA( ToolchainsBuildingRequest.class ) ) ).thenReturn( toolchainsResult );
        ToolchainPrivate basicToolchain = mock( ToolchainPrivate.class );
        when( toolchainFactory_basicType.createDefaultToolchain() ).thenReturn( basicToolchain );
        ToolchainPrivate rareToolchain = mock( ToolchainPrivate.class );
        when( toolchainFactory_rareType.createDefaultToolchain() ).thenReturn( rareToolchain );

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType( "unknown", session );

        // verify
        verify( logger ).error( "Missing toolchain factory for type: unknown. Possibly caused by misconfigured project." );
        assertEquals( 0, toolchains.length );
    }
    
    @Test
    public void testToolchainsForConfiguredType()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when( session.getRequest() ).thenReturn( req );

        PersistedToolchains effectiveToolchains = new PersistedToolchains();
        ToolchainModel basicToolchainModel = new ToolchainModel();
        basicToolchainModel.setType( "basic" );
        effectiveToolchains.addToolchain( basicToolchainModel );
        effectiveToolchains.addToolchain( basicToolchainModel );

        ToolchainModel rareToolchainModel = new ToolchainModel();
        rareToolchainModel.setType( "rare" );
        effectiveToolchains.addToolchain( rareToolchainModel );
        
        ToolchainsBuildingResult toolchainsResult = new DefaultToolchainsBuildingResult( effectiveToolchains, null );
        when( toolchainsBuilder.build( isA( ToolchainsBuildingRequest.class ) ) ).thenReturn( toolchainsResult );

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType( "basic", session );

        // verify
        verify( logger, never() ).error( anyString() );
        assertEquals( 2, toolchains.length );
    }
    
    @SuppressWarnings( "unchecked" )
    @Test( expected = MisconfiguredToolchainException.class )
    public void testMisconfiguredToolchain()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when( session.getRequest() ).thenReturn( req );

        when( toolchainsBuilder.build( isA( ToolchainsBuildingRequest.class ) ) ).thenThrow( ToolchainsBuildingException.class );
        
        // execute
        toolchainManager.getToolchainsForType( "basic", session );
        
        // verify
        fail( "Should exit with a MisconfiguredToolchainException" );
    }
    
    @Test
    public void testGlobalToolchainsFile()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        File globalFile = new File( "target/test-classes/org/apache/maven/toolchain/global.xml" );
        req.setGlobalToolchainsFile( globalFile );
        when( session.getRequest() ).thenReturn( req );

        ToolchainsBuildingResult toolchainsResult = new DefaultToolchainsBuildingResult( new PersistedToolchains(), null );
        when( toolchainsBuilder.build( isA( ToolchainsBuildingRequest.class ) ) ).thenReturn( toolchainsResult );
        
        // execute
        toolchainManager.getToolchainsForType( "basic", session );

        // verify
        ArgumentCaptor<ToolchainsBuildingRequest> argument = ArgumentCaptor.forClass(ToolchainsBuildingRequest.class);
        verify( toolchainsBuilder ).build( argument.capture() );
        assertNull( argument.getValue().getUserToolchainsSource() );
        assertEquals( globalFile.getAbsolutePath(), argument.getValue().getGlobalToolchainsSource().getLocation() );
    }

    @Test
    public void testUserToolchainsFile()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        File userFile = new File( "target/test-classes/org/apache/maven/toolchain/user.xml" );
        req.setUserToolchainsFile( userFile );
        when( session.getRequest() ).thenReturn( req );

        ToolchainsBuildingResult toolchainsResult = new DefaultToolchainsBuildingResult( new PersistedToolchains(), null );
        when( toolchainsBuilder.build( isA( ToolchainsBuildingRequest.class ) ) ).thenReturn( toolchainsResult );
        
        // execute
        toolchainManager.getToolchainsForType( "basic", session );

        // verify
        ArgumentCaptor<ToolchainsBuildingRequest> argument = ArgumentCaptor.forClass(ToolchainsBuildingRequest.class);
        verify( toolchainsBuilder ).build( argument.capture() );
        assertNull( argument.getValue().getGlobalToolchainsSource() );
        assertEquals( userFile.getAbsolutePath(), argument.getValue().getUserToolchainsSource().getLocation() );
    }

}
