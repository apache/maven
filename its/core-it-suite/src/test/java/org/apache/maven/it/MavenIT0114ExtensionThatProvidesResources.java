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
import java.util.List;

public class MavenIT0114ExtensionThatProvidesResources
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0114ExtensionThatProvidesResources()
    {
        super( "[,2.99.99)" );
    }

    public void testit0114()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/it0114-extensionThatProvidesResources" );

        Verifier verifier;

        // Install the parent POM, extension and the plugin 
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0114", "it0114-plugin-runner", "1.0", "pom" );                
        verifier.deleteArtifact( "org.apache.maven.its.it0114", "it0114-extension", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0114", "it0114-plugin", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0114", "it0114-parent", "1.0", "pom" );
        
        List cliOptions = new ArrayList();        
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        //now run the test
        testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/it0114-extensionThatProvidesResources/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        cliOptions = new ArrayList();
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
    }
}
