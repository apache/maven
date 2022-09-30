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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2103">MNG-2103</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2103PluginExecutionInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2103PluginExecutionInheritanceTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that the plugin-level inherited flag can be overridden by the execution-level flag.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2103" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "child-1/target" );
        verifier.deleteDirectory( "child-2/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> execs = verifier.loadLines( "child-1/target/log.txt", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "inherited" } ), execs );

        execs = verifier.loadLines( "child-2/target/log.txt", "UTF-8" );
        assertEquals( Arrays.asList( new String[] { "inherited" } ), execs );
    }

}
