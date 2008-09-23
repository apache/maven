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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0073Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Tests context passing between mojos in the same plugin.
     */
    public void testit0073()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0073" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-plugin-context-passing", "1.0",
                                 "maven-plugin" );
        List goals = Arrays.asList( new String[]{"org.apache.maven.its.plugins:maven-it-plugin-context-passing:throw",
            "org.apache.maven.its.plugins:maven-it-plugin-context-passing:catch"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/thrown-value" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

