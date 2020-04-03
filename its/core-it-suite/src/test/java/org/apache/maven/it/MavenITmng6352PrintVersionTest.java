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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * An integration test to check the enhancements to print out version
 * information during the reactor summary output at the correct
 * positions.
 *  
 * <a href="https://issues.apache.org/jira/browse/MNG-6352">MNG-6352</a>.
 * 
 * @author Karl Heinz Marbaise khmarbaise@apache.org
 */
public class MavenITmng6352PrintVersionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6352PrintVersionTest()
    {
        super( "[3.5.3,3.5.4]" );
    }

    /**
     * Check that the resulting output is
     * as expected for the root module and last 
     * module in build but not for the intermediate
     * modules. 
     */
    public void testitShouldPrintVersionAtTopAndAtBottom()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6352-print-version" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );
        
        verifier.setLogFileName( "version-log.txt" );
        verifier.executeGoals( Arrays.asList( "clean" ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> loadedLines = verifier.loadLines( "version-log.txt", "UTF-8" );
        List<String> resultingLines = extractReactorBuildOrder( loadedLines );

        // We expecting exactly four lines as result.
        assertEquals( 4, resultingLines.size() );

        // We expect those lines in the following exact order.
        assertTrue( resultingLines.get( 0 ).startsWith( "[INFO] base-project 1.3.0-SNAPSHOT ........................ SUCCESS [" ) );
        assertTrue( resultingLines.get( 1 ).startsWith( "[INFO] module-1 ........................................... SUCCESS [" ) );
        assertTrue( resultingLines.get( 2 ).startsWith( "[INFO] module-2 ........................................... SUCCESS [" ) );
        assertTrue( resultingLines.get( 3 ).startsWith( "[INFO] module-3 1.3.0-SNAPSHOT ............................ SUCCESS [" ) );
        
    }

    /**
     * Check that the resulting output is
     * as expected for all modules in case
     * for an aggregator build. 
     */
    public void testitShouldPrintVersionInAllLines()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6352-print-version-aggregator" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );
        
        verifier.setLogFileName( "version-log.txt" );
        verifier.executeGoals( Arrays.asList( "clean" ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> loadedLines = verifier.loadLines( "version-log.txt", "UTF-8" );
        List<String> resultingLines = extractReactorBuildOrder( loadedLines );

        // We expecting exactly four lines as result.
        assertEquals( 4, resultingLines.size() );

        // We expect those lines in the following exact order.
        assertTrue( resultingLines.get( 0 ).startsWith( "[INFO] module-1 1.2.7.43.RELEASE .......................... SUCCESS [  " ) );
        assertTrue( resultingLines.get( 1 ).startsWith( "[INFO] module-2 7.5-SNAPSHOT .............................. SUCCESS [  " ) );
        assertTrue( resultingLines.get( 2 ).startsWith( "[INFO] module-3 1-RC1 ..................................... SUCCESS [  " ) );
        assertTrue( resultingLines.get( 3 ).startsWith( "[INFO] base-project 1.0.0-SNAPSHOT ........................ SUCCESS [  " ) );

    }

    
    /**
     * Extract the lines at the end of the Maven output:
     * 
     * <pre>
     * [INFO] Reactor Summary:
     * [INFO]
     * [INFO] ...SUCCESS [  0.035 s]
     * [INFO] ...SUCCESS [  0.035 s]
     * [INFO] ...SUCCESS [  0.035 s]
     * </pre>
     */
    private List<String> extractReactorBuildOrder( List<String> loadedLines )
    {
        List<String> resultingLines = new LinkedList<String>();
        boolean start = false;
        for ( String line : loadedLines )
        {
            if ( start )
            {
                if ( line.startsWith( "[INFO] -------------" ) )
                {
                    start = false;
                }
                else if ( !line.endsWith( "[INFO] " ) )
                {
                    resultingLines.add( line );
                }
            }
            else
            {
                if ( line.startsWith( "[INFO] Reactor Summary:" ) )
                {
                    start = true;
                }

            }
        }
        return resultingLines;

    }

}
