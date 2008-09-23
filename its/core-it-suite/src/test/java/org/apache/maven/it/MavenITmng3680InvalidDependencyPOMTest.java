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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Verify that dependencies with invalid POMs can still be used without failing
 * the build.
 * 
 * @author jdcasey
 */
public class MavenITmng3680InvalidDependencyPOMTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3680InvalidDependencyPOMTest()
    {
        super( "(2.0.9,)" );
    }
    
    public void testitMNG3680 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3680-invalidDependencyPOM" );
        File pluginDir = new File( testDir, "maven-mng3680-plugin" );
        
        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( testDir.getAbsolutePath() );
        
        verifier.deleteArtifact( "tests", "dep-L1", "1", "jar" );
        verifier.deleteArtifact( "tests", "dep-L1", "1", "pom" );
        
        verifier.deleteArtifact( "tests", "dep-L1", "1", "jar" );
        verifier.deleteArtifact( "tests", "dep-L2", "1", "pom" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
