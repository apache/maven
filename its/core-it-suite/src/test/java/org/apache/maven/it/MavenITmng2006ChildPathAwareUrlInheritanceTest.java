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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2006">MNG-2006</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2006ChildPathAwareUrlInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2006ChildPathAwareUrlInheritanceTest()
    {
        super( "(2.0.2,)" );
    }

    /**
     * Test that inheritance of those URLs which automatically append the child's artifact id take the child's
     * relative location to the parent into account.
     */
    public void testitMNG2006()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2006" );

        Verifier verifier = newVerifier( new File( testDir, "child" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "http://project.url/child", props.getProperty( "project.url" ) );
        assertEquals( "http://viewvc.project.url/child", props.getProperty( "project.scm.url" ) );
        assertEquals( "http://scm.project.url/child", props.getProperty( "project.scm.connection" ) );
        assertEquals( "https://scm.project.url/child", props.getProperty( "project.scm.developerConnection" ) );
        if ( matchesVersionRange( "(2.0.7,)" ) )
        {
            // MNG-3134
            assertEquals( "http://site.project.url/child", props.getProperty( "project.distributionManagement.site.url" ) );
        }
    }

}
