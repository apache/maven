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
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Using a <code>${revision}</code> in the version will change the reactor order before fixing
 * <a href="https://issues.apache.org/jira/browse/MNG-6057">MNG-6057</a>. Without the fix for this issue the order of
 * the reactor is changed in that way that the parent is ordered to the last position instead of the first position.
 *
 * @author Karl Heinz Marbaise khmarbaise@apache.org
 */
public class MavenITmng6057CheckReactorOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6057CheckReactorOrderTest()
    {
        // The first version which contains the fix for the MNG-6057 issue.
        // TODO: Think about it!
        super( "[3.5.0-alpha-2,)" );
    }

    /**
     * Verify that the result shows the reactor order as expected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitReactorShouldResultInExpectedOrder()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6057-check-reactor-order" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setAutoclean( false );

        verifier.setLogFileName( "log-only.txt" );
        verifier.addCliArgument( "-Drevision=1.3.0-SNAPSHOT" );
        verifier.addCliArgument( "clean" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> loadedLines = verifier.loadLines( "log-only.txt", "UTF-8" );
        List<String> resultingLines = extractReactorBuildOrder( loadedLines );

        // We're expecting exactly three lines as result.
        assertEquals( 3, resultingLines.size() );

        // We expect those lines in the following exact order.
        assertTrue( resultingLines.get( 0 ).startsWith( "[INFO] base-project" ) );
        assertTrue( resultingLines.get( 1 ).startsWith( "[INFO] module-1" ) );
        assertTrue( resultingLines.get( 2 ).startsWith( "[INFO] module-2" ) );
    }

    /**
     * Extract the lines at the beginning of the Maven output:
     *
     * <pre>
     * [INFO] Reactor Build Order:
     * [INFO]
     * [INFO] module-1
     * [INFO] module-2
     * [INFO] base-project
     * [INFO]
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
                if ( line.startsWith( "[INFO] Reactor Build Order:" ) )
                {
                    start = true;
                }

            }
        }
        return resultingLines;

    }

}
