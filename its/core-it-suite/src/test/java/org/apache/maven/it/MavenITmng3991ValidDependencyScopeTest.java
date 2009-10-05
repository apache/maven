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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3991">MNG-3991</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng3991ValidDependencyScopeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3991ValidDependencyScopeTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that invalid dependency scopes cause a validation error during building.
     */
    public void testitProjectBuild()
        throws Exception
    {
        // TODO: One day, we should be able to error out but this requires to consider extensions and their use cases
        requiresMavenVersion( "[4.0,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3991/build" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Invalid dependency scope did not cause validation error" );
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
     * Test that invalid dependency scopes in dependency POMs are gracefully ignored.
     */
    public void testitMetadataRetrieval()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3991/metadata" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3991" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List classpath = verifier.loadLines( "target/test.txt", "UTF-8" );

        assertTrue( classpath.toString(), classpath.contains( "b-0.2.jar" ) );

        // In Maven 2.x, any dependency with an invalid/unrecognized scope ends up on the test class path...
        assertTrue( classpath.toString(), classpath.contains( "a-0.1.jar" ) );
    }

}
