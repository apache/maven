package org.apache.maven.model.profile.activation;

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

import java.io.File;
import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;

/**
 * Tests {@link JdkVersionProfileActivator}.
 *
 * @author Loïc B.
 */
public class FileProfileActivatorTest
    extends AbstractProfileActivatorTest<FileProfileActivator>
{

    public FileProfileActivatorTest()
    {
        super( FileProfileActivator.class );
    }

    private Profile newProfile( String exists, String missing )
    {
        ActivationFile file = new ActivationFile();
        file.setExists( exists );
        file.setMissing( missing );

        Activation a = new Activation();
        a.setFile( file );

        Profile p = new Profile();
        p.setActivation( a );

        return p;
    }

    @Override
    protected ProfileActivationContext newContext( Properties userProperties, Properties systemProperties )
    {
        DefaultProfileActivationContext context =
            (DefaultProfileActivationContext) super.newContext( userProperties, systemProperties );
        context.setProjectDirectory( new File( "." ) );
        return context;
    }
    public void testNullSafe()
        throws Exception
    {
        Profile p = new Profile();

        assertActivation( false, p, newContext( null, null ) );

        p.setActivation( new Activation() );

        assertActivation( false, p, newContext( null, null ) );
    }

    public void testSingleFile()
        throws Exception
    {
        checkActivation( true, "${basedir}/pom.xml", null );
        checkActivation( false, "!${basedir}/pom.xml", null );
        checkActivation( false, null, "${basedir}/pom.xml" );
        checkActivation( true, null, "!${basedir}/pom.xml" );

        checkActivation( false, "${basedir}/non-existent-file.xml", null );
        checkActivation( true, "!${basedir}/non-existent-file.xml", null );
        checkActivation( true, null, "${basedir}/non-existent-file.xml" );
        checkActivation( false, null, "!${basedir}/non-existent-file.xml" );

    }

    public void testAnd()
        throws Exception
    {
        checkActivation( true, "and(${basedir}/pom.xml,${basedir}/src)", null );
        checkActivation( false, "and(${basedir}/pom.xml,${basedir}/non-existent-file.xml)", null );

        checkActivation( true, "all(${basedir}/pom.xml,${basedir}/src)", null );
        checkActivation( false, "all(${basedir}/pom.xml,${basedir}/non-existent-file.xml)", null );

        checkActivation( false, null, "and(${basedir}/pom.xml,${basedir}/src)" );
        checkActivation( false, null, "and(${basedir}/pom.xml,${basedir}/non-existent-file.xml)" );
        checkActivation( true, null, "and(${basedir}/missing-file.xml,${basedir}/non-existent-file.xml)" );

        checkActivation( false, null, "all(${basedir}/pom.xml,${basedir}/src)" );
        checkActivation( false, null, "all(${basedir}/pom.xml,${basedir}/non-existent-file.xml)" );
        checkActivation( true, null, "all(${basedir}/missing-file.xml,${basedir}/non-existent-file.xml)" );
    }

    public void testOr()
        throws Exception
    {
        checkActivation( true, "or(${basedir}/pom.xml,${basedir}/src)", null );
        checkActivation( true, "or(${basedir}/pom.xml,${basedir}/non-existent-file.xml)", null );
        checkActivation( false, "or(${basedir}/missing-file.xml,${basedir}/non-existent-file.xml)", null );

        checkActivation( true, "any(${basedir}/pom.xml,${basedir}/src)", null );
        checkActivation( true, "any(${basedir}/pom.xml,${basedir}/non-existent-file.xml)", null );
        checkActivation( false, "any(${basedir}/missing-file.xml,${basedir}/non-existent-file.xml)", null );

        checkActivation( false, null, "or(${basedir}/pom.xml,${basedir}/src)" );
        checkActivation( true, null, "or(${basedir}/pom.xml,${basedir}/non-existent-file.xml)" );
        checkActivation( true, null, "or(${basedir}/missing-file.xml,${basedir}/non-existent-file.xml)" );

        checkActivation( false, null, "any(${basedir}/pom.xml,${basedir}/src)" );
        checkActivation( true, null, "any(${basedir}/pom.xml,${basedir}/non-existent-file.xml)" );
        checkActivation( true, null, "any(${basedir}/missing-file.xml,${basedir}/non-existent-file.xml)" );

    }

    public void testNegated()
        throws Exception
    {
        checkActivation( true, "all(${basedir}/pom.xml,!${basedir}/non-existent-file.xml)", null );
        checkActivation( true, "any(!${basedir}/pom.xml,!${basedir}/non-existent-file.xml)", null );
        checkActivation( false, "any(!${basedir}/pom.xml,${basedir}/non-existent-file.xml)", null );
        checkActivation( true, null, "all(!${basedir}/pom.xml,${basedir}/non-existent-file.xml)" );
        checkActivation( false, null, "any(${basedir}/pom.xml,!${basedir}/non-existent-file.xml)" );
    }

    private void checkActivation( boolean expected, String existing, String missing )
    {
        Profile profile = newProfile( existing, missing );
        assertActivation( expected, profile, newContext( null, null ) );
    }
}
