package org.apache.maven.integrationtests;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2068">MNG-2068</a>.
 *
 * Verify that a multimodule build, built from the middle node in an inheritance hierarchy,
 * can find all parent POMs necessary to build each project in the reactor using ONLY the
 * relativePath from the parent specification (in this case, the implied one of '../pom.xml').
 * 
 * @author jdcasey
 * 
 */
public class MavenITmng2068ReactorRelativeParentsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2068ReactorRelativeParentsTest()
    {
        super( "(2.0.6,)" ); // only test in 2.0.7+
    }

    public void testitMNG2068 ()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2068-reactorRelativeParents" );
        File projectDir = new File( testDir, "frameworks" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( projectDir.getAbsolutePath() );

        verifier.deleteArtifact( "samplegroup", "master", "0.0.1", "pom" );
        verifier.deleteArtifact( "samplegroup", "frameworks", "0.0.1", "pom" );
        verifier.deleteArtifact( "samplegroup", "core", "1.0.0", "pom" );

        verifier.executeGoal( "validate" );

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
