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

public class MavenIT0012PomInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0012PomInterpolationTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test simple POM interpolation
     *
     * @throws Exception in case of failure
     */
    public void testit0012()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0012" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "child-project/target" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/touch-3.8.1.txt" );
        verifier.assertFilePresent( "child-project/target/child-touch-3.0.3.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
