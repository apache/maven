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
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Test case for the {@link FileProfileActivator}.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class FileProfileActivatorTest
    extends TestCase
{
    private FileProfileActivator activator = new FileProfileActivator();

    public void testFileActivationProfile()
        throws ProfileActivationException
    {
        org.apache.maven.model.ActivationFile activationFile = new org.apache.maven.model.ActivationFile();

        // make an educated guess at the repository location...
        String repoLocation = System.getProperty( "maven.repo.local", "${user.home}/.m2/repository" );

        repoLocation = repoLocation.replace( '\\', '/' );
        if ( repoLocation.endsWith( "/" ) )
        {
            repoLocation = repoLocation.substring( 0, repoLocation.length() - 1 );
        }

        // Assume that junit exists
        activationFile.setExists( repoLocation + "/junit/junit/3.8.1/junit-3.8.1.jar" );

        Activation fileActivation = new Activation();
        fileActivation.setFile( activationFile );

        Profile profile = new Profile();
        profile.setActivation( fileActivation );

        Properties props = new Properties();
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        assertTrue( activator.isActive( profile, ctx ) );
    }
}
