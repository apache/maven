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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4768">MNG-4768</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4768NearestMatchConflictResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4768NearestMatchConflictResolutionTest()
    {
        super( "[2.0.9,)" );
    }

    // Ideally, all six permutations of the three direct dependencies should yield the same result...

    @Test
    public void testitABD()
        throws Exception
    {
        testit( "test-abd" );
    }

    @Test
    public void testitADB()
        throws Exception
    {
        requiresMavenVersion( "[3.0-beta-3,)" );
        testit( "test-adb" );
    }

    @Test
    public void testitBAD()
        throws Exception
    {
        testit( "test-bad" );
    }

    @Test
    public void testitBDA()
        throws Exception
    {
        testit( "test-bda" );
    }

    @Test
    public void testitDAB()
        throws Exception
    {
        requiresMavenVersion( "[3.0-beta-3,)" );
        testit( "test-dab" );
    }

    @Test
    public void testitDBA()
        throws Exception
    {
        testit( "test-dba" );
    }

    /**
     * Verify that conflict resolution picks the nearest version that matches all hard constraints given by ranges.
     * And for conflicting dependencies on distinct tree levels, "nearest" shouldn't be subject to the dependency
     * order.
     */
    private void testit( String test )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4768" );

        Verifier verifier = newVerifier( new File( testDir, test ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4768" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );

        assertTrue( test + " > " + classpath.toString(), classpath.contains( "a-2.0.jar" ) );
        assertTrue( test + " > " + classpath.toString(), classpath.contains( "b-0.1.jar" ) );
        assertTrue( test + " > " + classpath.toString(), classpath.contains( "c-0.1.jar" ) );
        assertTrue( test + " > " + classpath.toString(), classpath.contains( "d-0.1.jar" ) );

        assertFalse( test + " > " + classpath.toString(), classpath.contains( "a-2.1.jar" ) );
        assertFalse( test + " > " + classpath.toString(), classpath.contains( "a-1.0.jar" ) );
    }

}
