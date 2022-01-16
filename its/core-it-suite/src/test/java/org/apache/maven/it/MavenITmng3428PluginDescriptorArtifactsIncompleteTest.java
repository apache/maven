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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3428">MNG-3428</a>:
 * it tests that the PluginDescriptor.getArtifacts() call returns all of the dependencies of the plugin,
 * not just those that made it past the filter excluding Maven's core artifacts.
 */
public class MavenITmng3428PluginDescriptorArtifactsIncompleteTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3428PluginDescriptorArtifactsIncompleteTest()
    {
        // core artifacts are deliberately excluded in 3.x (see MNG-4277)
        super( "(2.0.8,3.0-alpha-1)" ); // 2.0.8+
    }

    public void testitMNG3428 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3428" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );

        // First, build the plugin we'll use to test the PluginDescriptor artifact collection.
        verifier.executeGoal( "install" );

        /*
         * This is the simplest way to check a build
         * succeeded. It is also the simplest way to create
         * an IT test: make the build pass when the test
         * should pass, and make the build fail when the
         * test should fail. There are other methods
         * supported by the verifier. They can be seen here:
         * http://maven.apache.org/shared/maven-verifier/apidocs/index.html
         */
        verifier.verifyErrorFreeLog();

        /*
         * Reset the streams before executing the verifier
         * again.
         */
        verifier.resetStreams();

        // This should only succeed if commons-cli is part of ${plugin.artifacts}. The
        // commons-cli library is part of Maven's core classpath, so if this mojo succeeds
        // it means the PluginDescriptor.getArtifacts() call returns an unfiltered collection.
        verifier.executeGoal( "org.apache.maven.its.mng3428:test-cli-maven-plugin:1:test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
