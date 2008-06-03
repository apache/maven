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
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3530">MNG-3530</a>.
 *
 * Tests that a modification by one plugin of the project.build.directory (in the project instance)
 * will be reflected in the value given to subsequent plugins that use ${project.build.directory}
 * in their configuration sections within the POM. If the changes are not propagated,
 * it represents an inconsistency between the project instance and the plugin configuration,
 * which is meant to reference that instance's state.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3530ChangedPathInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3530ChangedPathInterpolationTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.9,)" ); // only test in 2.0.9+
    }

    public void testitMNG3530 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3530-changedPathInterpolation" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project" );

        // First, install the plugin that modifies the project.build.directory and
        // validates that the modification propagated into the validation-mojo
        // configuration. Once this is installed, we can run a project build that
        // uses it to see how Maven will respond to a modification in the project build directory.
        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Now, build the project. If the plugin configuration doesn't recognize
        // the update to the project.build.directory, it will fail the build.
        verifier = new Verifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
