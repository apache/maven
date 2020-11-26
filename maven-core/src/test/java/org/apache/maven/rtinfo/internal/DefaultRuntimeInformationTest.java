package org.apache.maven.rtinfo.internal;

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

import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.apache.maven.PlexusTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import java.util.Collections;

public class DefaultRuntimeInformationTest
    extends PlexusTestCase
{
    @Inject
    RuntimeInformation rtInfo;

    @Override
    protected void customizeContainerConfiguration(
            ContainerConfiguration configuration)
    {
        super.customizeContainerConfiguration(configuration);
        configuration.setAutoWiring(true);
        configuration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    @Override
    protected synchronized void setupContainer()
    {
        super.setupContainer();

        ( (DefaultPlexusContainer) getContainer() ).addPlexusInjector( Collections.emptyList(),
                binder -> binder.requestInjection( this ) );
    }

    @Test
    public void testGetMavenVersion()
    {
        String mavenVersion = rtInfo.getMavenVersion();
        assertNotNull( mavenVersion );
        assertTrue( mavenVersion.length() > 0 );
    }

    @Test
    public void testIsMavenVersion()
    {
        assertTrue( rtInfo.isMavenVersion( "2.0" ) );
        assertFalse( rtInfo.isMavenVersion( "9.9" ) );

        assertTrue( rtInfo.isMavenVersion( "[2.0.11,2.1.0),[3.0,)" ) );
        assertFalse( rtInfo.isMavenVersion( "[9.0,)" ) );

        try
        {
            rtInfo.isMavenVersion( "[3.0," );
            fail( "Bad version range wasn't rejected" );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        try
        {
            rtInfo.isMavenVersion( "" );
            fail( "Bad version range wasn't rejected" );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        try
        {
            rtInfo.isMavenVersion( null );
            fail( "Bad version range wasn't rejected" );
        }
        catch ( NullPointerException e )
        {
            assertTrue( true );
        }
    }

}
