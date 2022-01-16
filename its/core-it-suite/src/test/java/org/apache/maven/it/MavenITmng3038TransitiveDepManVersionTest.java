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
import java.io.IOException;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3038">MNG-3038</a>
 *
 * @author Joakim Erdfelt
 *
 */
public class MavenITmng3038TransitiveDepManVersionTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3038TransitiveDepManVersionTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    public void testitMNG3038()
        throws Exception
    {
        File testDirBase = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3038" );

        compileDDep( testDirBase, "D1", "1.0" );
        compileDDep( testDirBase, "D2", "2.0" );

        File testProjectDir = new File( testDirBase, "test-project" );

        Verifier verifier = newVerifier( testProjectDir.getAbsolutePath() );
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
        Verifier verifierOtherDep = newVerifier( testOtherDepDir.getAbsolutePath() );
        verifierOtherDep.deleteArtifact( "org.apache.maven.its.it0121", "D", version, "jar" );
        verifierOtherDep.deleteArtifact( "org.apache.maven.its.it0121", "D", version, "pom" );
        verifierOtherDep.executeGoal( "install" );
        verifierOtherDep.verifyErrorFreeLog();
        verifierOtherDep.resetStreams();
    }
}
