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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3671">MNG-3671</a>.
 *
 * Tests to make sure that custom plugin dependencies (defined in your local POM) have
 * their information interpolated before they are injected into the plugin descriptor
 * for eventual resolution and inclusion in the plugin classpath. Otherwise, resolution
 * errors will occur and the plugin will fail to initialize.
 *
 * @author jdcasey
 *
 */
public class MavenITmng3671PluginLevelDepInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3671PluginLevelDepInterpolationTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    @Test
    public void testitMNG3671 ()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3671" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "compile" );

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
    }
}
