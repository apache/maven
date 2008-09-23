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

public class MavenIT0042Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that the reactor can establish the artifact location of known projects for dependencies
     */
    public void testit0042()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0042" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "test-component-a/target/test-component-a-0.1.jar" );
        verifier.assertFilePresent( "test-component-b/target/test-component-b-0.1.jar" );
        verifier.assertFilePresent( "test-component-c/target/test-component-c-0.1.war" );
        verifier.assertFilePresent(
            "test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar" );
        verifier.assertFilePresent(
            "test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-b-0.1.jar" );
        verifier.assertFilePresent( "test-component-c/target/my-test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

