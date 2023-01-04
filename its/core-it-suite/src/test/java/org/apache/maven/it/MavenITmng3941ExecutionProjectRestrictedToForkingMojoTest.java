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
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3941">MNG-3941</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3941ExecutionProjectRestrictedToForkingMojoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3941ExecutionProjectRestrictedToForkingMojoTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Test that the execution project from a forked lifecycle does not leak into mojos that run after the mojo
     * that forked the lifecycle. While this is rather irrelevant for Maven's core itself, this enforces proper
     * mojo programming, i.e. a mojo should not access the execution project unless it forked the lifecycle.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3941" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/fork.properties" );
        assertEquals( "test-0.1", props.getProperty( "executedProject.build.finalName" ) );
    }

}
