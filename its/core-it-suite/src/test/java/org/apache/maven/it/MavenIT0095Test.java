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

public class MavenIT0095Test
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0095Test()
    {
        super( "[,2.99.99)" );
    }

    /**
     * Test URL calculation when modules are in sibling dirs of parent. (MNG-2006)
     */
    public void testit0095()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0095" );
        File sub1 = new File( testDir, "sub1" );

        Verifier verifier = new Verifier( sub1.getAbsolutePath() );
        Properties systemProperties = new Properties();
        systemProperties.put( "expression.expressions", "project/scm" );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/expression.properties" );
        Properties props = verifier.loadProperties( "target/expression.properties" );
        assertEquals( "scm:svn:http://svn.apache.org/repos/asf/maven/it0095/sub1", 
                      props.getProperty( "project.scm.connection" ) );
        assertEquals( "scm:svn:https://svn.apache.org/repos/asf/maven/it0095/sub1", 
                      props.getProperty( "project.scm.developerConnection" ) );
        assertEquals( "http://svn.apache.org/repos/asf/maven/it0095/sub1", 
                      props.getProperty( "project.scm.url" ) );
    }

}
