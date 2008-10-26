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

public class MavenITmng3391ImportScopeErrorScenariosTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3391ImportScopeErrorScenariosTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3391a()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391/depMgmt-importPom-noParentCycle" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3391.2", "dm-pom", "1", "pom" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }

    public void testitMNG3391b()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391/depMgmt-importPom-noParentCycle" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3391.2", "dm-pom", "1", "pom" );

        Verifier v2 = new Verifier( new File( testDir, "dm-pom" ).getAbsolutePath() );
        v2.executeGoal( "install" );
        v2.verifyErrorFreeLog();
        v2.resetStreams();

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG3391c()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391/depMgmt-importPom-parentCycle" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3391.1", "dm-pom", "1", "pom" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }

    public void testitMNG3391d()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391/depMgmt-importPom-parentCycle" );

        Verifier verifier = new Verifier( new File( testDir, "dm-pom" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
