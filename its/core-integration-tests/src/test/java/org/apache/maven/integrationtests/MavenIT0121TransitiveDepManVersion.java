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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0121TransitiveDepManVersion
    extends AbstractMavenIntegrationTestCase
{
    public void testit0121()
        throws Exception
    {
        File testDirBase = ResourceExtractor.simpleExtractResources( getClass(), "/it0121-transitiveDepManVersion" );

        compileDDep( testDirBase, "D1", "1.0" );
        compileDDep( testDirBase, "D2", "2.0" );

        File testProjectDir = new File( testDirBase, "test-project" );
        
        Verifier verifier = new Verifier( testProjectDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0121", "A", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0121", "A", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0121", "B", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0121", "B", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0121", "C", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0121", "D", "1.0", "jar" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    private void compileDDep( File testDirBase, String projectDDepDir, String version )
        throws VerificationException, IOException
    {
        File testOtherDepDir = new File( testDirBase, "test-other-deps/" + projectDDepDir );
        Verifier verifierOtherDep = new Verifier( testOtherDepDir.getAbsolutePath() );
        verifierOtherDep.deleteArtifact( "org.apache.maven.its.it0121", "D", version, "jar" );
        verifierOtherDep.deleteArtifact( "org.apache.maven.its.it0121", "D", version, "pom" );
        verifierOtherDep.executeGoal( "install" );
        verifierOtherDep.verifyErrorFreeLog();
        verifierOtherDep.resetStreams();
    }
}
