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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2739">MNG-2739</a>.
 * todo Fill in a better description of what this test verifies!
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 */
public class MavenITmng2739RequiredRepositoryElementsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2739RequiredRepositoryElementsTest()
    {
        super( "(2.0.9,)" ); // only test in 2.0.9+
    }

    @Test
    public void testitMNG2739_RepositoryId()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2739/repo-id" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "validate" );

            fail(
                "POM should NOT validate: repository <id/> element is missing in: " + new File( testDir, "pom.xml" ) );
        }
        catch ( VerificationException e )
        {
            // expected
        }


        List<String> listing = verifier.loadFile( new File( testDir, "log.txt" ), false );
        boolean foundNpe = false;
        for ( String line : listing )
        {
            if ( line.contains( "NullPointerException" ) )
            {
                foundNpe = true;
                break;
            }
        }

        assertFalse( "Missing repository-id should not result in a NullPointerException.", foundNpe );
    }

    @Test
    public void testitMNG2739_RepositoryUrl()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2739/repo-url" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "validate" );

            fail(
                "POM should NOT validate: repository <url/> element is missing in: " + new File( testDir, "pom.xml" ) );
        }
        catch ( VerificationException e )
        {
            // expected
        }


        List<String> listing = verifier.loadFile( new File( testDir, "log.txt" ), false );
        boolean foundNpe = false;
        for ( String line : listing )
        {
            if ( line.contains( "NullPointerException" ) )
            {
                foundNpe = true;
                break;
            }
        }

        assertFalse( "Missing repository-url should not result in a NullPointerException.", foundNpe );
    }
}
