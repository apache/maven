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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * An integration test to check the enhancements to print out version
 * information during the reactor summary output at the correct
 * positions.
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-6391">MNG-6391</a>.
 *
 * @author Karl Heinz Marbaise khmarbaise@apache.org
 */
public class MavenITmng6391PrintVersionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6391PrintVersionTest()
    {
        super( "[3.6.0,)" );
    }

    /**
     * Check that the resulting output is
     * as expected for the root module and last
     * module in build but not for the intermediate
     * modules.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitShouldPrintVersionAtTopAndAtBottom()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6391-print-version" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );

        verifier.setLogFileName( "version-log.txt" );
        verifier.executeGoals( Arrays.asList( "clean" ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> loadedLines = verifier.loadLines( "version-log.txt", "UTF-8" );
        List<String> resultingLines = extractReactorBuildOrder( loadedLines );

        // We're expecting exactly four lines as result.
        assertEquals( 5, resultingLines.size() );

        // We expect those lines in the following exact order.
        // Maven 4.0.x has some code new code which takes care of the terminal width to adjust the output.
        // The number of dots can thus vary when running the tests.
        assertTrue( resultingLines.get( 0 ).startsWith( "[INFO] Reactor Summary for base-project 1.3.0-SNAPSHOT:" ) );
        assertTrue( resultingLines.get( 1 ).matches( "\\Q[INFO] base-project ............\\E\\.+\\Q SUCCESS [\\E.*" ) );
        assertTrue( resultingLines.get( 2 ).matches( "\\Q[INFO] module-1 ................\\E\\.+\\Q SUCCESS [\\E.*" ) );
        assertTrue( resultingLines.get( 3 ).matches( "\\Q[INFO] module-2 ................\\E\\.+\\Q SUCCESS [\\E.*" ) );
        assertTrue( resultingLines.get( 4 ).matches( "\\Q[INFO] module-3 ................\\E\\.+\\Q SUCCESS [\\E.*" ) );

        // We expect that line 1..4 have the same length
        int line1Length = resultingLines.get( 1 ).length();
        assertEquals( line1Length, resultingLines.get( 2 ).length() );
        assertEquals( line1Length, resultingLines.get( 3 ).length() );
        assertEquals( line1Length, resultingLines.get( 4 ).length() );

    }

    /**
     * Check that the resulting output is
     * as expected for all modules in case
     * for an aggregator build.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitShouldPrintVersionInAllLines()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6391-print-version-aggregator" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );

        verifier.setLogFileName( "version-log.txt" );
        verifier.executeGoals( Arrays.asList( "clean" ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> loadedLines = verifier.loadLines( "version-log.txt", "UTF-8" );
        List<String> resultingLines = extractReactorBuildOrder( loadedLines );

        // We're expecting exactly four lines as result.
        assertEquals( 5, resultingLines.size() );

        // We expect those lines in the following exact order.
        // Maven 4.0.x has some code new code which takes care of the terminal width to adjust the output.
        // The number of dots can thus vary when running the tests.
        assertTrue( resultingLines.get( 0 ).startsWith( "[INFO] Reactor Summary:" ) );
        assertTrue( resultingLines.get( 1 ).matches( "\\Q[INFO] module-1 1.2.7.43.RELEASE ...............\\E\\.+\\Q SUCCESS [ \\E.*" ) );
        assertTrue( resultingLines.get( 2 ).matches( "\\Q[INFO] module-2 7.5-SNAPSHOT ...................\\E\\.+\\Q SUCCESS [ \\E.*" ) );
        assertTrue( resultingLines.get( 3 ).matches( "\\Q[INFO] module-3 1-RC1 ..........................\\E\\.+\\Q SUCCESS [ \\E.*" ) );
        assertTrue( resultingLines.get( 4 ).matches( "\\Q[INFO] base-project 1.0.0-SNAPSHOT .............\\E\\.+\\Q SUCCESS [ \\E.*" ) );

        // We expect that line 1..4 have the same length
        int line1Length = resultingLines.get( 1 ).length();
        assertEquals( line1Length, resultingLines.get( 2 ).length() );
        assertEquals( line1Length, resultingLines.get( 3 ).length() );
        assertEquals( line1Length, resultingLines.get( 4 ).length() );

    }

    /**
     * Extract the lines at the end of the Maven output:
     *
     * <pre>
     * [INFO] Reactor Summary..: XXX
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
                if ( line.startsWith( "[INFO] Reactor Summary" ) )
                {
                    start = true;
                    resultingLines.add( line );
                }

            }
        }
        return resultingLines;

    }

}
