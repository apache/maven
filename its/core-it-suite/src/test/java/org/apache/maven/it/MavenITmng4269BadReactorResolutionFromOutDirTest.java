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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4269">MNG-4269</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MavenITmng4269BadReactorResolutionFromOutDirTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4269BadReactorResolutionFromOutDirTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that dependency resolution from the reactor is not too eager and does not resolve plugin artifacts from
     * the build directory of their plugin project when the plugin project hasn't been built yet. The technical
     * problem is that the mere existence of a project output directory like target/classes is no sufficient indicator
     * that we can use that for artifact resolution. The project's output directory might just be a left over from a
     * previous build and could be in any state, e.g. incomplete.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4269" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        // NOTE: It's a crucial prerequisite to create the output directory, i.e. the bad choice
        new File( testDir, "target/classes" ).mkdirs();
        verifier.deleteArtifacts( "org.apache.maven.its.mng4269" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        // This should use the previous installation/deployment from the repo, not the invalid output directory
        verifier.executeGoal( "org.apache.maven.its.mng4269:maven-mng4269-plugin:0.1:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
    }

}
