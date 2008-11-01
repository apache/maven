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
import java.util.Collection;

public class MavenIT0083Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that overriding a transitive compile time dependency as provided in a WAR ensures it is not included.
     */
    public void testit0083()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0083" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it0083" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Collection compileArtifacts = verifier.loadLines( "target/compile.txt", "UTF-8" );
        assertTrue( compileArtifacts.toString(), 
            compileArtifacts.contains( "org.apache.maven.its.it0083:direct-dep:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), 
            compileArtifacts.contains( "org.apache.maven.its.it0083:trans-dep:jar:0.1" ) );

        Collection runtimeArtifacts = verifier.loadLines( "target/runtime.txt", "UTF-8" );
        assertTrue( runtimeArtifacts.toString(), 
            runtimeArtifacts.contains( "org.apache.maven.its.it0083:direct-dep:jar:0.1" ) );
        assertFalse( runtimeArtifacts.toString(), 
            runtimeArtifacts.contains( "org.apache.maven.its.it0083:trans-dep:jar:0.1" ) );
    }

}
