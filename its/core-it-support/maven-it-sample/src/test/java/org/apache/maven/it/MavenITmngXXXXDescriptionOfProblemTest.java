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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources directory along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources directory. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmngXXXXDescriptionOfProblemTest
    extends AbstractMavenIntegrationTestCase
{

    // TODO: RENAME THIS TEST TO SUIT YOUR SCENARIO.
    // Usign the Jira issue id this reproduces is a good
    // start, along with a description:
    // ie MavenITmngXXXXHoustonWeHaveAProblemTest  (must end in test)
    public MavenITmngXXXXDescriptionOfProblemTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    @Test
    public void testitMNGxxxx ()
        throws Exception
    {

        // The testdir is computed from the location of this
        // file.
        // TODO: RENAME THIS PATH TO MATCH YOUR ISSUE ID.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-xxxx" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.itsample", "parent", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.itsample", "checkstyle-test", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.itsample", "checkstyle-assembly", "1.0", "jar" );

        /*
         * The Command Line Options (CLI) are passed to the
         * verifier as a list. This is handy for things like
         * redefining the local repository if needed. In
         * this case, we use the -N flag so that Maven won't
         * recurse. We are only installing the parent pom to
         * the local repo here.
         */
        List cliOptions = new ArrayList();
        cliOptions.add( "-N" );
        verifier.setCliOptions( cliOptions );
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
         * This particular test requires an extension
         * containing resources to be installed that is then
         * used by the actual IT test. Here we invoker Maven
         * again to install it. Again, this is just
         * preparation for the test.
         */
        verifier = new Verifier( new File( testDir.getAbsolutePath(), "checkstyle-assembly" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        /*
         * Now we are running the actual test. This
         * particular test will attempt to load the
         * resources from the extension jar previously
         * installed. If Maven doesn't pass this to the
         * classpath correctly, the build will fail. This
         * particular test will fail in Maven <2.0.6.
         */
        verifier = new Verifier( new File( testDir.getAbsolutePath(), "checkstyle-test" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        /*
         * The verifier also supports beanshell scripts for
         * verification of more complex scenarios. There are
         * plenty of examples in the core-it tests here:
         * http://svn.apache.org/repos/asf/maven/core-integration-testing/trunk
         */
    }
}
