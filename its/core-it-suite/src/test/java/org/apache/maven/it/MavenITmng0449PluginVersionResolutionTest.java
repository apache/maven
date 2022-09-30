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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-449">MNG-449</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0449PluginVersionResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0449PluginVersionResolutionTest()
    {
        super( "[2.0,)" );
    }

    /**
     * Verify that versions for plugins are automatically resolved if not given in the POM by checking first LATEST and
     * then RELEASE in the repo metadata when the plugin is invoked from the lifecycle.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitLifecycleInvocation()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0449" );
        testDir = new File( testDir, "lifecycle" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.deleteArtifacts( "org.apache.maven.its.mng0449" );
        }
        catch ( Exception e )
        {
            // when we run Maven embedded, the plugin class realm locks the artifacts so we can't delete them
        }
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Maven 3.x prefers RELEASE over LATEST (see MNG-4206)
        if ( matchesVersionRange( "(,3.0-alpha-3)" ) )
        {
            verifier.verifyFileNotPresent( "target/touch-release.txt" );
            verifier.verifyFilePresent( "target/touch-snapshot.txt" );
        }
        else
        {
            verifier.verifyFilePresent( "target/touch-release.txt" );
            verifier.verifyFileNotPresent( "target/touch-snapshot.txt" );
        }
        verifier.verifyFilePresent( "target/package.txt" );
    }

    /**
     * Verify that versions for plugins are automatically resolved if not given in the POM by checking LATEST and
     * RELEASE in the repo metadata when the plugin is invoked directly from the command line.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCliInvocation()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0449" );
        testDir = new File( testDir, "direct" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.deleteArtifacts( "org.apache.maven.its.mng0449" );
        }
        catch ( Exception e )
        {
            // when we run Maven embedded, the plugin class realm locks the artifacts so we can't delete them
        }
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "org.apache.maven.its.mng0449:maven-it-plugin-a:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Maven 3.x prefers RELEASE over LATEST (see MNG-4206)
        if ( matchesVersionRange( "(,3.0-alpha-3)" ) )
        {
            verifier.verifyFileNotPresent( "target/touch-release.txt" );
            verifier.verifyFilePresent( "target/touch-snapshot.txt" );
        }
        else
        {
            verifier.verifyFilePresent( "target/touch-release.txt" );
            verifier.verifyFileNotPresent( "target/touch-snapshot.txt" );
        }
    }

}
