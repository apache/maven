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


public class MavenITmng2861RelocationsAndRangesTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2861RelocationsAndRangesTest()
    {
        super( "(2.0.8,)" );
    }

    public void testitMNG2861 ()
        throws Exception
    {


        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2861" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( new File(testDir,"MNG-2861").getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng2861", "MNG-2861", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2861", "A", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2861", "B", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2861", "C", "1.0-SNAPSHOT", "jar" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

    }
}
