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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4023">MNG-4023</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4023ParentProfileOneTimeInjectionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4023ParentProfileOneTimeInjectionTest()
    {
        super( "[2.0.11,2.1.0-M1),[2.1.0-M2,)" );
    }

    /**
     * Verify that profiles in a parent are only injected once during a reactor build that include the parent
     * itself. The parent being part of the reactor makes it subject to project caching and proper use of the
     * cache is crucial here.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG4023()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4023" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "sub/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "sub/target/config.properties" );
        assertEquals( "1", props.getProperty( "stringParams" ) );
        assertEquals( "test", props.getProperty( "stringParams.0" ) );
        assertNull( props.getProperty( "stringParams.1" ) );
    }

}
