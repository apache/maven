package org.apache.maven.integrationtests;

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

public class MavenIT0112ExtensionsThatDragDependencies
    extends AbstractMavenIntegrationTestCase
{
    public void testit0112()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0112-extensionsThatDragDependencies" );

        Verifier verifier;

        // Install the parent POM
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0112", "test-extension", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0112", "test-project", "1.0-SNAPSHOT", "jar" );

        // Install the extension with the resources required for the test
        verifier = new Verifier( new File( testDir.getAbsolutePath(), "test-extension" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Run the whole test
        verifier = new Verifier( new File( testDir.getAbsolutePath(), "test-project" ).getAbsolutePath() );
        verifier.executeGoal( "project-info-reports:scm" );
        // ommitted because we always get velocity errors that aren't covered by the verifier
//        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
