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
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4383">MNG-4383</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng4383ValidDependencyVersionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4383ValidDependencyVersionTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that non-interpolated dependency versions cause a validation error during building.
     */
    public void testitProjectBuild()
        throws Exception
    {
        requiresMavenVersion( "[3.0-alpha-3,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4383/build" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Invalid dependency version did not cause validation error" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    /**
     * Test that non-interpolated dependency versions in dependency POMs are gracefully ignored (as long as those
     * dependencies are not actually resolved).
     */
    public void testitMetadataRetrieval()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4383/metadata" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4383" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List classpath = verifier.loadLines( "target/test.txt", "UTF-8" );

        assertTrue( classpath.toString(), classpath.contains( "b-0.2.jar" ) );
        assertTrue( classpath.toString(), classpath.contains( "a-0.1.jar" ) );
    }

}
