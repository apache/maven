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
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4800">MNG-4800</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4800NearestWinsVsScopeWideningTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4800NearestWinsVsScopeWideningTest()
    {
        super( "[3.0-beta-4,)" );
    }

    @Test
    public void testitAB()
        throws Exception
    {
        testit( "test-ab" );
    }

    @Test
    public void testitBA()
        throws Exception
    {
        testit( "test-ba" );
    }

    /**
     * Verify that nearest-wins conflict resolution doesn't get confused when a farther conflicting dependency has
     * a wider scope than the nearer dependency, i.e. one should still end up with the nearer dependency (s:1) and
     * its subtree (x) but in the wider scope (compile).
     */
    private void testit( String test )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4800" );

        Verifier verifier = newVerifier( new File( testDir, test ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4800" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compile = verifier.loadLines( "target/compile.txt", "UTF-8" );

        assertTrue( test + " > " + compile.toString(), compile.contains( "b-0.1.jar" ) );
        assertTrue( test + " > " + compile.toString(), compile.contains( "c-0.1.jar" ) );
        assertTrue( test + " > " + compile.toString(), compile.contains( "s-0.1.jar" ) );
        assertTrue( test + " > " + compile.toString(), compile.contains( "x-0.1.jar" ) );
        assertFalse( test + " > " + compile.toString(), compile.contains( "a-0.1.jar" ) );
        assertFalse( test + " > " + compile.toString(), compile.contains( "s-0.2.jar" ) );
        assertFalse( test + " > " + compile.toString(), compile.contains( "y-0.1.jar" ) );

        List<String> runtime = verifier.loadLines( "target/runtime.txt", "UTF-8" );

        assertTrue( test + " > " + runtime.toString(), runtime.contains( "b-0.1.jar" ) );
        assertTrue( test + " > " + runtime.toString(), runtime.contains( "c-0.1.jar" ) );
        assertTrue( test + " > " + runtime.toString(), runtime.contains( "s-0.1.jar" ) );
        assertTrue( test + " > " + runtime.toString(), runtime.contains( "x-0.1.jar" ) );
        assertTrue( test + " > " + runtime.toString(), runtime.contains( "a-0.1.jar" ) );
        assertFalse( test + " > " + runtime.toString(), runtime.contains( "s-0.2.jar" ) );
        assertFalse( test + " > " + runtime.toString(), runtime.contains( "y-0.1.jar" ) );
    }

}
