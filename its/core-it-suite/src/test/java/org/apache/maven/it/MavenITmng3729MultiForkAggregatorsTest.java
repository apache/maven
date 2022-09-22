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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3729">MNG-3729</a>.
 * <p>
 * Complicated use case, but say
 * you have an aggregator plugin that forks a lifecycle, and this aggregator is bound to the main lifecycle in a
 * multi-module build. Further, say you call another plugin directly from the command line for this multi-module build,
 * which forks a new lifecycle (like assembly:assembly).
 * </p>
 * When the directly invoked aggregator forks, it will force the
 * forked lifecycle phase to be run for each project in the reactor, regardless of whether this causes the bound
 * aggregator mojo to run multiple times. When the bound aggregator executes for the first project (this will be in an
 * inner fork, two levels removed from the main lifecycle execution) it will set the executionProject to null for the
 * current project (which is one of the reactorProjects member instances). On the second pass, as it tries to execute
 * the inner aggregator's forked lifecycle for the second project in the reactor, one of the reactorProjects'
 * project.getExecutionProject() results will be null. If any of the mojos in this inner lifecycle fork requires
 * dependency resolution, it will cause a NullPointerException in the DefaultPluginManager when it tries to resolve the
 * dependencies for the current project. This happened in 2.0.10-RC11 (which was the predecessor to 2.1.0-RC12, since
 * the version was renamed while the release process was in mid-execution). It did not happen in 2.0.9, and was fixed in
 * 2.1.0-RC12.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 */
public class MavenITmng3729MultiForkAggregatorsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3729MultiForkAggregatorsTest()
    {
        super( "(2.0.8,3.0-alpha-1),[3.0-alpha-3,)" ); // only test in 2.0.9+
    }

    public void testitMNG3729 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3729" );
        File pluginDir = new File( testDir, "maven-mng3729-plugin" );
        File projectDir = new File( testDir, "projects" );

        Verifier verifier;

        verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
