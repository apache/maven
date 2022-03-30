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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Properties;

import org.apache.maven.internal.xml.Xpp3Dom;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

public class DefaultToolchainTest
{
    private final Logger logger = mock( Logger.class );

    @BeforeEach
    public void setUp()
        throws Exception
    {
        MockitoAnnotations.initMocks( this );
    }

    private DefaultToolchain newDefaultToolchain( ToolchainModel model )
    {
        return new DefaultToolchain( model, logger )
        {
            @Override
            public String findTool( String toolName )
            {
                return null;
            }
        };
    }

    private DefaultToolchain newDefaultToolchain( ToolchainModel model, String type )
    {
        return new DefaultToolchain( model, type, logger )
        {
            @Override
            public String findTool( String toolName )
            {
                return null;
            }
        };
    }

    @Test
    public void testGetModel()
    {
        ToolchainModel model = ToolchainModel.newInstance();
        DefaultToolchain toolchain = newDefaultToolchain( model );
        assertEquals( model, toolchain.getModel() );
    }

    @Test
    public void testGetType()
    {
        ToolchainModel model = ToolchainModel.newInstance();
        DefaultToolchain toolchain = newDefaultToolchain( model, "TYPE" );
        assertEquals( "TYPE", toolchain.getType() );

        model = model.withType( "MODEL_TYPE" );
        toolchain = newDefaultToolchain( model );
        assertEquals( "MODEL_TYPE", toolchain.getType() );
    }

    @Test
    public void testGetLogger()
    {
        ToolchainModel model = ToolchainModel.newInstance();
        DefaultToolchain toolchain = newDefaultToolchain( model );
        assertEquals( logger, toolchain.getLog() );
    }

    @Test
    public void testMissingRequirementProperty()
    {
        ToolchainModel model = ToolchainModel.newInstance();
        model = model.withType( "TYPE" );
        DefaultToolchain toolchain = newDefaultToolchain( model );

        assertFalse( toolchain.matchesRequirements( Collections.singletonMap( "name", "John Doe" ) ) );
        verify( logger ).debug( "Toolchain type:TYPE{} is missing required property: name" );
    }


    @Test
    public void testNonMatchingRequirementProperty()
    {
        ToolchainModel model = ToolchainModel.newInstance();
        model = model.withType( "TYPE" );
        DefaultToolchain toolchain = newDefaultToolchain( model );
        toolchain.addProvideToken( "name", RequirementMatcherFactory.createExactMatcher( "Jane Doe" ) );

        assertFalse( toolchain.matchesRequirements( Collections.singletonMap( "name", "John Doe" ) ) );
        verify( logger ).debug( "Toolchain type:TYPE{name = Jane Doe} doesn't match required property: name" );
    }


    @Test
    public void testEquals()
    {
        Properties props1 = new Properties();
        props1.put( "version", "1.5" );
        props1.put( "vendor", "sun" );
        Xpp3Dom jdkHome1 = new Xpp3Dom( "jdkHome", "${env.JAVA_HOME}", null, null, null );
        Xpp3Dom configuration1 = new Xpp3Dom("configuration", null, null, Collections.singletonList( jdkHome1 ), null );
        ToolchainModel tm1 = ToolchainModel.newBuilder()
                .type( "jdk" )
                .provides( props1 )
                .configuration( configuration1 )
                .build();

        Properties props2 = new Properties();
        props2.put( "version", "1.4" );
        props2.put( "vendor", "sun" );
        Xpp3Dom jdkHome2 = new Xpp3Dom( "jdkHome", "${env.JAVA_HOME}", null, null, null );
        Xpp3Dom configuration2 = new Xpp3Dom("configuration", null, null, Collections.singletonList( jdkHome2 ), null );
        ToolchainModel tm2 = ToolchainModel.newBuilder()
                .type( "jdk" )
                .provides( props2 )
                .configuration( configuration2 )
                .build();

        DefaultToolchain tc1 = new DefaultJavaToolChain( tm1, null );
        DefaultToolchain tc2 = new DefaultJavaToolChain( tm2, null );

        assertEquals( tc1, tc1 );
        assertNotEquals( tc1, tc2 );
        assertNotEquals( tc2, tc1 );
        assertEquals( tc2, tc2 );
    }
}
