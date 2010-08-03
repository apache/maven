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
import java.util.Arrays;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources folder along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources folder. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng3372DirectInvocationOfPluginsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3372DirectInvocationOfPluginsTest()
    {
        super( "(2.0.5,)" );
    }

    public void testitMNG3372()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testBaseDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3372/direct-using-prefix" );
        File plugin = new File( testBaseDir, "plugin" );
        File project = new File( testBaseDir, "project" );
        File settingsFile = new File( testBaseDir, "settings.xml" );

        Verifier verifier = newVerifier( plugin.getAbsolutePath(), "remote" );

        verifier.deleteArtifacts( "org.apache.maven.its.mng3372" );

        verifier.getSystemProperties().setProperty( "updateReleaseInfo", "true" );

        verifier.executeGoals( Arrays.asList( new String[]{ "clean", "install" } ) );

        verifier = newVerifier( project.getAbsolutePath() );

        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( "\"" + settingsFile.getAbsolutePath() + "\"" );

        verifier.executeGoal( "mng3372:test" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }

    public void testDependencyTreeInvocation()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testBaseDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3372/dependency-tree" );

        Verifier verifier = newVerifier( testBaseDir.getAbsolutePath(), "remote" );

        verifier.getCliOptions().add( "-U" );

        verifier.executeGoal( "dependency:tree" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }
}
