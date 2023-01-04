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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3331">MNG-3331</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng3331ModulePathNormalizationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3331ModulePathNormalizationTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    @Test
    public void testitMNG3331a ()
        throws Exception
    {
        //testMNG3331ModuleWithSpaces
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3331/with-spaces" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "initialize" );

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
    }

    @Test
    public void testitMNG3331b ()
        throws Exception
    {
        //testMNG3331ModuleWithRelativeParentDirRef
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3331/with-relative-parentDir-ref" );

        Verifier verifier;

        verifier = newVerifier( new File( testDir, "parent" ).getAbsolutePath() );

        verifier.executeGoal( "initialize" );

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
    }

}
