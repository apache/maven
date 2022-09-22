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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-761">MNG-761</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0761MissingSnapshotDistRepoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0761MissingSnapshotDistRepoTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that a deployment of a snapshot falls back to a non-snapshot repository if no snapshot repository is
     * specified.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG761()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0761" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng0761" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/repo/org/apache/maven/its/mng0761/test/1.0-SNAPSHOT/test-1.0-*.jar" );
    }

}
