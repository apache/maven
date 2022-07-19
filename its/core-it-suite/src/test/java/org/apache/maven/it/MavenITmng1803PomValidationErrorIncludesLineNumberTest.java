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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1803">MNG-1803</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1803PomValidationErrorIncludesLineNumberTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1803PomValidationErrorIncludesLineNumberTest()
    {
        super( "[3.0-beta-2,)" );
    }

    /**
     * Verify that POM errors indicate the line and column number in the input file.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1803" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        try
        {
            verifier.executeGoal( "validate" );
        }
        catch ( Exception e )
        {
            // expected
        }
        finally
        {
            verifier.resetStreams();
        }

        boolean foundError = false;

        List<String> lines = verifier.loadLines( verifier.getLogFileName(), null );
        for ( String line : lines )
        {
            if ( line.contains( ":bad/id:" ) )
            {
                assertTrue( "Line number not found in: " + line, line.indexOf( "38" ) > 0 );
                assertTrue( "Column number not found in: " + line, line.indexOf( "19" ) > 0 );
                foundError = true;
                break;
            }
        }

        assertTrue( "Build output did not mention validation error!", foundError );
    }

}
