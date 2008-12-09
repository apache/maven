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
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0129ResourceProvidedToAPluginAsAPluginDependencyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0129ResourceProvidedToAPluginAsAPluginDependencyTest()
    {
        super( "(2.0.3,)" );
    }

    public void testit0129()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/it0129" );

        Verifier verifier;

        // Install the parent POM, extension and the plugin
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-plugin-runner", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-extension", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-plugin", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-parent", "1.0", "pom" );

        List cliOptions = new ArrayList();
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        //now run the test
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0129/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        cliOptions = new ArrayList();
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}