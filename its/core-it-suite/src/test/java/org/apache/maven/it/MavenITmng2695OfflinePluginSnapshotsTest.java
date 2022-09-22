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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2695">MNG-2695</a>.
 *
 * Verifies that offline mode functions correctly for snapshot plugins.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 */
public class MavenITmng2695OfflinePluginSnapshotsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2695OfflinePluginSnapshotsTest()
    {
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,)" ); // only test in 2.0.10+, and not in 2.1.0-M1
    }

    /**
     * Verify that snapshot plugins which are scheduled for an update don't fail the build when in offline mode.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG2695()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2695" );

        {
            // phase 1: run build in online mode to fill local repo
            Verifier verifier = newVerifier( testDir.getAbsolutePath() );
            verifier.deleteDirectory( "target" );
            verifier.deleteArtifacts( "org.apache.maven.its.mng2695" );
            verifier.setAutoclean( false );
            verifier.setLogFileName( "log1.txt" );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
            verifier.verifyFilePresent( "target/a.txt" );
            verifier.verifyFilePresent( "target/b.txt" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }

        {
            // phase 2: run build in offline mode to check it still passes
            Verifier verifier = newVerifier( testDir.getAbsolutePath() );
            verifier.deleteDirectory( "target" );
            verifier.setAutoclean( false );
            verifier.setLogFileName( "log2.txt" );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.addCliOption( "--offline" );
            verifier.executeGoal( "validate" );
            verifier.verifyFilePresent( "target/a.txt" );
            verifier.verifyFilePresent( "target/b.txt" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
    }

}
