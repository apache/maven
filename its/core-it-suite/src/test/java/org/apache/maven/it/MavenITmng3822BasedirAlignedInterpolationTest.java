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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3822">MNG-3822</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3822BasedirAlignedInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3822BasedirAlignedInterpolationTest()
    {
        super( "[2.1.0-M1,)");
    }

    /**
     * Verify that POM interpolation uses basedir-aligned build directories.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3822()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3822" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pomProps = verifier.loadProperties( "target/interpolated.properties" );
        assertEquals( testDir, "src/main/java", pomProps.getProperty( "project.properties.buildMainSrc" ) );
        assertEquals( testDir, "src/test/java", pomProps.getProperty( "project.properties.buildTestSrc" ) );
        assertEquals( testDir, "src/main/scripts", pomProps.getProperty( "project.properties.buildScriptSrc" ) );
        assertEquals( testDir, "target", pomProps.getProperty( "project.properties.buildOut" ) );
        assertEquals( testDir, "target/classes", pomProps.getProperty( "project.properties.buildMainOut" ) );
        assertEquals( testDir, "target/test-classes", pomProps.getProperty( "project.properties.buildTestOut" ) );
        assertEquals( testDir, "target/site", pomProps.getProperty( "project.properties.siteOut" ) );
    }

    private void assertEquals( File testDir, String buildDir, String interpolatedPath )
        throws Exception
    {
        File actual = new File( interpolatedPath );
        File expected = new File( testDir, buildDir );

        assertTrue( actual.isAbsolute() );
        assertEquals( expected.getCanonicalFile(), actual.getCanonicalFile() );
    }

}
