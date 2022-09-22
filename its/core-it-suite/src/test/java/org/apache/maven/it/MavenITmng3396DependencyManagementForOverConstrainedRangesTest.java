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

import java.io.File;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3396">MNG-3396</a>.
 *
 *
 */
public class MavenITmng3396DependencyManagementForOverConstrainedRangesTest
    extends AbstractMavenIntegrationTestCase
{
    private static final String GROUP_ID = "org.apache.maven.its.mng3396";

    public MavenITmng3396DependencyManagementForOverConstrainedRangesTest()
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    public void testitMNG3396()
        throws Exception
    {
        String baseDir = "/mng-3396";
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), baseDir + "/dependencies" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.deleteArtifact( GROUP_ID, "A", "1.0", "pom" );
        verifier.deleteArtifact( GROUP_ID, "A", "1.0", "jar" );
        verifier.deleteArtifact( GROUP_ID, "B", "1.0", "pom" );
        verifier.deleteArtifact( GROUP_ID, "B", "1.0", "jar" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), baseDir + "/plugin" );

        verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.deleteArtifact( GROUP_ID, "A", "1.0", "pom" );
        verifier.deleteArtifact( GROUP_ID, "A", "1.0", "jar" );
        verifier.deleteArtifact( GROUP_ID, "A", "3.0", "pom" );
        verifier.deleteArtifact( GROUP_ID, "A", "3.0", "jar" );
        verifier.deleteArtifact( GROUP_ID, "plugin", "1.0", "pom" );
        verifier.deleteArtifact( GROUP_ID, "plugin", "1.0", "jar" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), baseDir + "/pluginuser" );

        verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.deleteArtifact( GROUP_ID, "pluginuser", "1.0", "pom" );
        verifier.deleteArtifact( GROUP_ID, "pluginuser", "1.0", "jar" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
