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

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4418">MNG-4418</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng4418ReservedRepoIdLocalTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4418ReservedRepoIdLocalTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Test that remote repositories may not use the id "local" which is reserved for the local repository.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4418" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Reserved id 'local' should have caused model validation error." );
        }
        catch ( VerificationException e )
        {
            verifier.verifyTextInLog( "must not be 'local'" );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

}
