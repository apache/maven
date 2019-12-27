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
import java.io.IOException;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3475">MNG-3475</a> and
 * <a href="https://issues.apache.org/jira/browse/MNG-1927">MNG-1927</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3475BaseAlignedDirTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3475BaseAlignedDirTest()
    {
        super( "(2.0.1,2.0.3),(2.0.3,)");
    }

    /**
     * Verify that project directories are basedir aligned when queried by plugin parameter expressions.
     */
    public void testitMNG3475()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3475" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties configProps = verifier.loadProperties( "target/config.properties" );

        assertPathEquals( testDir, "target", configProps.getProperty( "mapParam.buildDirectory" ) );

        assertPathEquals( testDir, "target/classes", configProps.getProperty( "mapParam.buildOutputDirectory" ) );

        assertPathEquals( testDir, "target/test-classes", configProps.getProperty( "mapParam.buildTestOutputDirectory" ) );

        assertPathEquals( testDir, "src/main/java", configProps.getProperty( "mapParam.buildSourceDirectory" ) );

        assertPathEquals( testDir, "src/test/java", configProps.getProperty( "mapParam.buildTestSourceDirectory" ) );

        if ( matchesVersionRange( "[2.1.0-M1,)" ) )
        {
            assertPathEquals( testDir, "target/site", configProps.getProperty( "mapParam.reportingOutputDirectory" ) );
        }

        // show that using relative paths is aligned for File configuration properties regardless
        assertPathEquals( testDir, "target/site", configProps.getProperty( "fileParam" ) );

        /*
         * NOTE: The script source directory is deliberately excluded from the checks due to MNG-3741.
         */
    }

    private void assertPathEquals( File basedir, String expected, String actual )
        throws IOException
    {
        File actualFile = new File( actual );
        assertTrue( "path not absolute: " + actualFile, actualFile.isAbsolute() );
        assertCanonicalFileEquals( new File( basedir, expected ), actualFile );
    }

}
