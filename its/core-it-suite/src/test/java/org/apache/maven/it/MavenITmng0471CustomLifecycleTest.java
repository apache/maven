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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-471">MNG-471</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0471CustomLifecycleTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0471CustomLifecycleTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test @execute with a custom lifecycle, including configuration
     */
    public void testitMNG471()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0471" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        List<String> goals = Arrays.asList( new String[]{"org.apache.maven.its.plugins:maven-it-plugin-fork:fork",
            "org.apache.maven.its.plugins:maven-it-plugin-fork:fork-goal"} );
        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/forked/touch.txt" );
        verifier.assertFilePresent( "target/forked2/touch.txt" );
    }

}
