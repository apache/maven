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

/**
 * # it0091 currrently fails. Not sure if there is an associated JIRA.
 */
public class MavenIT0091Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that currently demonstrates that properties are not correctly
     * interpolated into other areas in the POM. This may strictly be a boolean
     * problem: I captured the problem as it was reported.
     */
    public void testit0091()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0091" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.assertFilePresent( "target/classes/test.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

