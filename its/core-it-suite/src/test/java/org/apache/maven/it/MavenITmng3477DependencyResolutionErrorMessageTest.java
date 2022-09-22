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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3477">MNG-3477</a>.
 */
public class MavenITmng3477DependencyResolutionErrorMessageTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3477DependencyResolutionErrorMessageTest()
    {
        super( "[2.1.0,3.0-alpha-1),[3.0-beta-1,)" );
    }

    /**
     * Tests that dependency resolution errors tell the underlying transport issue.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3477" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3477" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        try
        {
            verifier.executeGoal( "validate" );
            fail( "Build should have failed to resolve dependency" );
        }
        catch ( VerificationException e )
        {
            boolean foundCause = false;
            List<String> lines = verifier.loadLines( verifier.getLogFileName(), "UTF-8" );
            for ( String line : lines )
            {
                if ( line.matches( ".*org.apache.maven.its.mng3477:dep:jar:1.0.*Connection.*refused.*" ) )
                {
                    foundCause = true;
                    break;
                }
            }
            assertTrue( "Transfer error cause was not found", foundCause );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

}
