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
import org.apache.maven.model.Profile;

import junit.framework.TestCase;

public class JdkPrefixProfileActivatorTest
    extends TestCase
{
    public void testDetectionWithoutActivationElement()
    {
        JdkPrefixProfileActivator activator = createActivator();

        Profile profile = new Profile();

        assertFalse( activator.canDetectActivation( profile ) );
    }

    public void testDetectionWithoutJdkElement()
    {
        JdkPrefixProfileActivator activator = createActivator();

        Profile profile = createProfile( null );

        assertFalse( activator.canDetectActivation( profile ) );
    }

    public void testDetectionWithJdkElement()
    {
        JdkPrefixProfileActivator activator = createActivator();

        Profile profile = createProfile( "1.5" );

        assertTrue( activator.canDetectActivation( profile ) );
    }

    public void testActivationWithMatchingJdkVersion()
    {
        JdkPrefixProfileActivator activator = createActivator( "1.5.0_15" );

        Profile profile = createProfile( "1.5" );

        assertTrue( activator.isActive( profile ) );
    }

    public void testActivationWithoutMatchingJdkVersion()
    {
        JdkPrefixProfileActivator activator = createActivator( "1.5.0_15" );

        Profile profile = createProfile( "1.4" );

        assertFalse( activator.isActive( profile ) );
    }

    public void testActivationWithMatchingNegatedJdkVersion()
    {
        JdkPrefixProfileActivator activator = createActivator( "1.5.0_15" );

        Profile profile = createProfile( "!1.4" );

        assertTrue( activator.isActive( profile ) );
    }

    public void testActivationWithoutMatchingNegatedJdkVersion()
    {
        JdkPrefixProfileActivator activator = createActivator( "1.5.0_15" );

        Profile profile = createProfile( "!1.5" );

        assertFalse( activator.isActive( profile ) );
    }

    private static JdkPrefixProfileActivator createActivator()
    {
        return new JdkPrefixProfileActivator();
    }

    private static JdkPrefixProfileActivator createActivator( final String testJdkVersion )
    {
        return new JdkPrefixProfileActivator()
        {
            protected String getJdkVersion()
            {
                return testJdkVersion;
            }
        };
    }

    private static Profile createProfile( String jdk )
    {
        Profile profile = new Profile();
        Activation activation = new Activation();
        activation.setJdk( jdk );
        profile.setActivation( activation );
        return profile;
    }
}
