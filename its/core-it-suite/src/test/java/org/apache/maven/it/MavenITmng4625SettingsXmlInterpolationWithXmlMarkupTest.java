package org.apache.maven.it;

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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.codehaus.plexus.util.Os;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4625">MNG-4625</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4625SettingsXmlInterpolationWithXmlMarkupTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4625SettingsXmlInterpolationWithXmlMarkupTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that interpolation of the settings.xml doesn't fail if an expression's value contains
     * XML special characters.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4625" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6468220
        // A lot of bugs related to Windows arguments and quoting
        // Directly called from commandline succeeds, indirect often fails
        if( Os.isFamily( Os.FAMILY_WINDOWS ) && !System.getProperties().contains( "CLASSWORLDS_LAUNCHER" ) )
        {
            verifier.setSystemProperty( "test.prop", "\"&x=y<>\"" );
            verifier.setForkJvm( true ); // force forked JVM, since the workaround expects forked run
        }
        else
        {
            verifier.setSystemProperty( "test.prop", "&x=y<>" );
        }

        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "&x=y<>", props.getProperty( "project.properties.jdbcUrl" ) );
    }

}
