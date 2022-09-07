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

import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test to make sure that the reactor is properly constrained when --projects is used. Previous to 3.1.2 all of the
 * projects found in the {@code <modules/>} section of the POM were passed into the reactor. This test is a 5 project
 * multi-module project where only project-0, and project-1 are specified to be used. The project-0 has a dependency on
 * project-4 and in this constrained mode the dependency resolution should fail because project-4 is no longer placed in
 * the reactor.
 *
 * @author jvanzyl
 */
public class MavenITmng5557ProperlyRestrictedReactor
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5557ProperlyRestrictedReactor()
    {
        super( "[3.1.2,)" );
    }

    public void testRunningRestrictedReactor()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5557-properly-restricted-reactor" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        //
        // Remove everything related to this project from the local repository as we want this to be resolution purely
        // from the reactor.
        //
        verifier.deleteArtifacts( "org.apache.maven.its.mng5557" );
        verifier.addCliOption( "--projects" );
        verifier.addCliOption( "project-0,project-1" );
        try
        {
            verifier.executeGoal( "package" );
        }
        catch ( VerificationException e )
        {
            // the execution should fail due to a resolution error.
        }
        verifier.resetStreams();
        verifier.verifyTextInLog( "Could not resolve dependencies for project org.apache.maven.its.mng5557:project-0:jar:1.0: Could not find artifact org.apache.maven.its.mng5557:project-4:jar:1.0" );
    }
}
