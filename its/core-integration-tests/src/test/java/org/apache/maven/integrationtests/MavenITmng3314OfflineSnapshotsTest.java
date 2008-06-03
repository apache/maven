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

package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3314">MNG-3314</a>.
 *
 * Verifies that offline mode functions correctly for snapshot dependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3314OfflineSnapshotsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3314OfflineSnapshotsTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    /**
     * Consists of two projects. First is a dependency project with a snapshot
     * version, which is deployed to the url 'file:../test-repo'. Second is a
     * project that declares a dependency on the first project, and also declares
     * a repository entry pointing to the url 'file:../test-repo' (these projects
     * are in sibling directories, so this should be the same location as the
     * first project deployed to). The second project's repository declaration
     * specifies releases disabled, and snapshots enabled with an update-policy
     * of 'always', which should trigger metadata updates on each build.
     * <br />
     * This test has four parts, executed in order:
     * <ol>
     *   <li>Deploy a snapshot dependency to the test repository, then remove
     *       the dependency from the local repository.</li>
     *
     *   <li>Build a project using that dependency using offline mode, without
     *       the dependency in the local repository. This build should fail.</li>
     *
     *   <li>Build the project again without offline mode. The dependency should
     *       be downloaded from the test repository, and the build should succeed.</li>
     *
     *   <li>Build the project once more using offline mode. The dependency should
     *       exist in the local repository, so the build should still succeed.</li>
     * </ol>
     */
    public void testitMNG3314 ()
        throws Exception
    {
        File testParentDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3314-offlineSnapshots" );

        File testDep = new File( testParentDir, "dep" );

        Verifier verifier = new Verifier( testDep.getAbsolutePath() );

        // Deploy the dependency to the test repository.
        verifier.executeGoal( "deploy" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String depPath = verifier.getArtifactPath( "org.apache.maven.its.mng3314", "dep", "1.0-SNAPSHOT", "pom" );

        File dep = new File( depPath );
        dep = dep.getParentFile().getParentFile();

        // remove the dependency from the local repository.
        FileUtils.deleteDirectory( dep );

        File testProject = new File( testParentDir, "project" );

        verifier = new Verifier( testProject.getAbsolutePath() );

        // Conditions for this build:
        // 1. dependency is NOT in local repository
        // 2. executing in offline mode
        //
        // Expected outcome: build failure
        try
        {
            List cliOptions = new ArrayList();
            cliOptions.add( "-o" );
            verifier.setCliOptions( cliOptions );

            verifier.executeGoal( "compile" );

            fail( "Dependency should be missing from local repo, and in offline this should make the project build fail." );
        }
        catch( VerificationException e )
        {
            // should fail.
        }
        finally
        {
            verifier.resetStreams();
        }

        // move this log file off to a new name to make room for the next build.
        File buildLog = new File( testProject, "log.txt" );
        if ( buildLog.exists() )
        {
            buildLog.renameTo( new File( testProject, "log-build1.txt" ) );
        }

        // Conditions for this build:
        // 1. dependency is NOT in local repository
        // 2. executing in ONLINE mode
        //
        // Expected outcome: build success
        try
        {
            verifier.getCliOptions().clear();

            verifier.executeGoal( "compile" );
        }
        finally
        {
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }

        // move this log file off to a new name to make room for the next build.
        if ( buildLog.exists() )
        {
            buildLog.renameTo( new File( testProject, "log-build2.txt" ) );
        }

        // Conditions for this build:
        // 1. dependency IS in local repository
        // 2. executing in offline mode
        //
        // Expected outcome: build success
        try
        {
            List cliOptions = new ArrayList();
            cliOptions.add( "-o" );
            verifier.setCliOptions( cliOptions );

            verifier.executeGoal( "compile" );
        }
        finally
        {
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }

    }
}
