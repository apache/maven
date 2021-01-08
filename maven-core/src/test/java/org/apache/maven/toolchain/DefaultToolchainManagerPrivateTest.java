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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultToolchainManagerPrivateTest
{
    // Mocks to inject into toolchainManager
    @Mock
    private Logger logger;

    @InjectMocks
    private DefaultToolchainManagerPrivate toolchainManager;

    @Mock
    private ToolchainFactory toolchainFactory_basicType;

    @Mock
    private ToolchainFactory toolchainFactory_rareType;

    @BeforeEach
    public void setUp()
    {
        toolchainManager = new DefaultToolchainManagerPrivate();

        MockitoAnnotations.initMocks( this );

        toolchainManager.factories = new HashMap<>();
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
        Map<String, List<ToolchainModel>> groupedToolchains = new HashMap<>();
        req.setToolchains( groupedToolchains );

        List<ToolchainModel> basicToolchains = new ArrayList<>();
        ToolchainModel basicToolchainModel = new ToolchainModel();
        basicToolchainModel.setType( "basic" );
        basicToolchains.add( basicToolchainModel );
        basicToolchains.add( basicToolchainModel );
        groupedToolchains.put( "basic", basicToolchains );

        List<ToolchainModel> rareToolchains = new ArrayList<>();
        ToolchainModel rareToolchainModel = new ToolchainModel();
        rareToolchainModel.setType( "rare" );
        rareToolchains.add( rareToolchainModel );
        groupedToolchains.put( "rare", rareToolchains );

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType( "basic", session );

        // verify
        verify( logger, never() ).error( anyString() );
        assertEquals( 2, toolchains.length );
    }

    @Test
    public void testMisconfiguredToolchain()
        throws Exception
    {
        // prepare
        MavenSession session = mock( MavenSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when( session.getRequest() ).thenReturn( req );

        // execute
        ToolchainPrivate[] basics = toolchainManager.getToolchainsForType("basic", session);

        // verify
        assertEquals( 0, basics.length );
    }
}
