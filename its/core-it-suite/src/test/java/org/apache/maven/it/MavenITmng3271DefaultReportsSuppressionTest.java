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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3271">MNG-3271</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3271DefaultReportsSuppressionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3271DefaultReportsSuppressionTest()
    {
        // reporting is not supported in 3.x core (see MNG-4162)
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,3.0-alpha-1)" );
    }

    /**
     * Test that default reports can be suppressed.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3271()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3271" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/reports.properties" );
        assertEquals( "0", props.getProperty( "reports" ) );
        assertNull( props.getProperty( "reports.0" ) );
    }

}
