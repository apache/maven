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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5000">MNG-5000</a>. Note this is a subtle
 * variation and not a duplicate of the test for MNG-2006.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng5000ChildPathAwareUrlInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5000ChildPathAwareUrlInheritanceTest()
    {
        super( "[2.0.11,2.0.99),[2.2.0,3.0-alpha-1),[3.0.3,)" );
    }

    /**
     * Verify that child path aware URL adjustment still works when the child's artifactId doesn't match the name
     * of its base directory as given in the parent's module section.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5000" );

        Verifier verifier = newVerifier( new File( testDir, "different-from-artifactId" ).getAbsolutePath() );
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
        assertEquals( "http://site.project.url/child", props.getProperty( "project.distributionManagement.site.url" ) );
    }

}
