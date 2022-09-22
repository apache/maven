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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4190">MNG-4190</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4190MirrorRepoMergingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4190MirrorRepoMergingTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Test that artifact repositories are merged if they are mirrored by the same repo. If n repos map to one
     * mirror, there is no point in making n trips to the same mirror. However, the effective/merged repo needs
     * to account for possibly different policies of the original repos.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4190" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4190" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        Collections.sort( artifacts );

        List<String> expected = new ArrayList<>();
        expected.add( "org.apache.maven.its.mng4190:a:jar:0.1" );
        expected.add( "org.apache.maven.its.mng4190:b:jar:0.1-SNAPSHOT" );

        assertEquals( expected, artifacts );

        Properties props = verifier.loadProperties( "target/repo.properties" );
        assertEquals( "1", props.getProperty( "project.remoteArtifactRepositories" ) );

        assertEquals( "true", props.getProperty( "project.remoteArtifactRepositories.0.releases.enabled" ) );
        assertEquals( "ignore", props.getProperty( "project.remoteArtifactRepositories.0.releases.checksumPolicy" ) );
        assertEquals( "daily", props.getProperty( "project.remoteArtifactRepositories.0.releases.updatePolicy" ) );

        assertEquals( "true", props.getProperty( "project.remoteArtifactRepositories.0.snapshots.enabled" ) );
        assertEquals( "ignore", props.getProperty( "project.remoteArtifactRepositories.0.snapshots.checksumPolicy" ) );
        assertEquals( "always", props.getProperty( "project.remoteArtifactRepositories.0.snapshots.updatePolicy" ) );
    }

}
