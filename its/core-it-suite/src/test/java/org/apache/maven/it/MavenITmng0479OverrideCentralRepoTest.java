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

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-479">MNG-479</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng0479OverrideCentralRepoTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0479OverrideCentralRepoTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test for repository inheritance - ensure using the same id overrides the defaults
     */
    public void testitMNG479()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0479" );

        // Phase 1: Ensure the test plugin is downloaded before the test cuts off access to central
        File child1 = new File( testDir, "setup" );
        Verifier verifier = newVerifier( child1.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Phase 2: Now run the test
        File child2 = new File( testDir, "test" );
        verifier = newVerifier( child2.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.filterFile( "settings.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.getCliOptions().add( "--settings" );
        verifier.getCliOptions().add( "settings.xml" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/expression.properties" );
        Properties props = verifier.loadProperties( "target/expression.properties" );

        int count = Integer.parseInt( props.getProperty( "project.repositories", "0" ) );
        assertTrue( count > 0 );
        for ( int i = 0; i < count; i++ )
        {
            String key = "project.repositories." + i;
            if ( "central".equals( props.getProperty( key + ".id" ) ) )
            {
                assertEquals( "it0043", props.getProperty( key + ".name" ) );
                assertTrue( props.getProperty( key + ".url" ).endsWith( "/target/maven-core-it0043-repo" ) );
            }
        }

        count = Integer.parseInt( props.getProperty( "project.pluginRepositories", "0" ) );
        for ( int i = 0; i < count; i++ )
        {
            String key = "project.pluginRepositories." + i;
            if ( "central".equals( props.getProperty( key + ".id" ) ) )
            {
                assertEquals( "it0043", props.getProperty( key + ".name" ) );
                assertTrue( props.getProperty( key + ".url" ).endsWith( "/target/maven-core-it0043-repo" ) );
            }
        }
    }

}
