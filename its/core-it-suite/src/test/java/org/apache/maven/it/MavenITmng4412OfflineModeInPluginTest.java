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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4412">MNG-4412</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4412OfflineModeInPluginTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4412OfflineModeInPluginTest()
    {
        super( "[2.0,3.0-alpha-1),[3.0-alpha-4,)" );
    }

    /**
     * Verify that plugins using the 2.x style artifact resolver directly are subject to the offline mode of the
     * current Maven session.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitResolver()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4412" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4412" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-Presolver" );
        verifier.addCliOption( "--offline" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.setLogFileName( "log-resolver.txt" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Plugin could resolve artifact from remote repository despite Maven being offline" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
    }

    /**
     * Verify that plugins using the 2.x style artifact collector directly are subject to the offline mode of the
     * current Maven session.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCollector()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4412" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4412" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-Pcollector" );
        verifier.addCliOption( "--offline" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.setLogFileName( "log-collector.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng4412", "dep", "0.1", "pom" );
    }

}
