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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1999">MNG-1999</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng1999DefaultReportsInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1999DefaultReportsInheritanceTest()
    {
        // reporting not supported by Maven 3.x core as per MNG-4162
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,3.0-alpha-1)" ); // 2.0.10+, excluding 2.1.0-M1
    }

    /**
     * Test that default reports can be suppressed via inheritance from the parent.
     *
     * @throws Exception in case of failure
     */
    public void testitInheritSuppression()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1999" );

        Verifier verifier = newVerifier( new File( testDir, "child1" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/reports.properties" );
        assertEquals( "0", props.getProperty( "reports" ) );
        assertNull( props.getProperty( "reports.0" ) );
    }

    /**
     * Verify that children can re-enable default reports if suppressed via inheritance from the parent.
     *
     * @throws Exception in case of failure
     */
    public void testitOverrideSuppression()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1999" );

        Verifier verifier = newVerifier( new File( testDir, "child2" ).getAbsolutePath(), "remote" );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/reports.properties" );
        props = verifier.loadProperties( "target/reports.properties" );
        assertNotNull( props.getProperty( "reports.0" ) );
    }

}
