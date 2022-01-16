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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3530">MNG-3530</a>.
 *
 * Contains various tests for dynamism of interpolation expressions within the POM.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 *
 */
public class MavenITmng3530DynamicPOMInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    private static final String BASEDIR = "/mng-3530/";

    public MavenITmng3530DynamicPOMInterpolationTest()
    {   //Dynamic properties for forked lifecycles not supported in 3.0
        super( "[2.1.0-M1,3.0-alpha-1)" );
    }

    public void testitMNG3530_BuildPath()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR + "build-path" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project" );

        // First, install the plugin that modifies the project.build.directory and
        // validates that the modification propagated into the validation-mojo
        // configuration. Once this is installed, we can run a project build that
        // uses it to see how Maven will respond to a modification in the project build directory.
        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Now, build the project. If the plugin configuration doesn't recognize
        // the update to the project.build.directory, it will fail the build.
        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG3530_POMProperty()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR
                                                                             + "pom-property" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project" );

        // First, install the plugin that modifies the myDirectory and
        // validates that the modification propagated into the validation-mojo
        // configuration. Once this is installed, we can run a project build that
        // uses it to see how Maven will respond to a modification in the POM property.
        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Now, build the project. If the plugin configuration doesn't recognize
        // the update to the myDirectory, it will fail the build.
        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG3530_ResourceDirectoryInterpolation()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR
                                                                             + "resource-object" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project" );

        // First, install the plugin which validates that all resource directory
        // specifications have been interpolated.
        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Now, build the project. If the plugin finds an uninterpolated resource
        // directory, it will fail the build.
        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
