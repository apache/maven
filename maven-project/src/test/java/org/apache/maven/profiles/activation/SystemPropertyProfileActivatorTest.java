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

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;

import java.util.Properties;

import junit.framework.TestCase;

public class SystemPropertyProfileActivatorTest
    extends TestCase
{

    private SystemPropertyProfileActivator activator = new SystemPropertyProfileActivator();

    public void testCanDetect_ShouldReturnTrueWhenActivationPropertyIsPresent()
        throws Exception
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        Properties props = new Properties();
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        assertTrue( activator.canDetermineActivation( profile, ctx ) );
    }

    public void testCanDetect_ShouldReturnFalseWhenActivationPropertyIsNotPresent()
        throws Exception
    {
        Activation activation = new Activation();

        Profile profile = new Profile();

        profile.setActivation( activation );

        Properties props = new Properties();
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        assertFalse( activator.canDetermineActivation( profile, ctx ) );
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

        Properties props = new Properties();
        props.setProperty( "test", "true" );
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        assertTrue( activator.isActive( profile, ctx ) );
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

        Properties props = new Properties();
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        assertFalse( activator.isActive( profile, ctx ) );
    }

}
