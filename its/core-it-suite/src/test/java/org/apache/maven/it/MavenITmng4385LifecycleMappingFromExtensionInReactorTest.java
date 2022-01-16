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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4385">MNG-4385</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4385LifecycleMappingFromExtensionInReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4385LifecycleMappingFromExtensionInReactorTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that custom lifecycle mappings contributed by build extensions of one project do not leak into other
     * projects in the reactor.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4385" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Build leaked lifecycle mapping from extension of first project into second project!" );
        }
        catch( VerificationException e )
        {
            // expected, should fail
            String msg = e.getMessage();

            assertTrue( "Failure should be due to unknown packaging", msg.contains( "Unknown packaging: it-packaging" ));
            assertTrue( "Failure should be due to sub-b project", msg.contains( "The project org.apache.maven.its.mng4385:sub-b:0.1" ));
        }
        verifier.resetStreams();
    }

}
