package org.apache.maven.profiles.activation;

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

import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.DefaultBuildContextManager;
import org.apache.maven.context.SystemBuildContext;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Properties;

public class SystemPropertyProfileActivatorTest
    extends PlexusTestCase
{
    
    private BuildContextManager buildContextManager;
    private SystemPropertyProfileActivator activator;
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        buildContextManager = (BuildContextManager) lookup( BuildContextManager.ROLE, DefaultBuildContextManager.ROLE_HINT );
        
        SystemBuildContext sysContext = SystemBuildContext.getSystemBuildContext( buildContextManager, true );
        sysContext.store( buildContextManager );
        
        activator = (SystemPropertyProfileActivator) lookup( ProfileActivator.ROLE, "system-property" );
    }

    public void testCanDetect_ShouldReturnTrueWhenActivationPropertyIsPresent()
        throws Exception
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        assertTrue( activator.canDetermineActivation( profile ) );
    }

    public void testCanDetect_ShouldReturnFalseWhenActivationPropertyIsNotPresent()
        throws Exception
    {
        Activation activation = new Activation();

        Profile profile = new Profile();

        profile.setActivation( activation );

        assertFalse( activator.canDetermineActivation( profile ) );
    }

    public void testIsActive_ShouldReturnTrueWhenPropertyNameSpecifiedAndPresent()
        throws Exception
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        System.setProperty( "test", "true" );

        assertTrue( activator.isActive( profile ) );
    }

    public void testIsActive_ShouldReturnFalseWhenPropertyNameSpecifiedAndMissing()
        throws Exception
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        Properties props = System.getProperties();
        props.remove( "test" );
        System.setProperties( props );

        assertFalse( activator.isActive( profile ) );
    }

}
