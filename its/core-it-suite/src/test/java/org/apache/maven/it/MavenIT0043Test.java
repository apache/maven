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

public class MavenIT0043Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test for repository inheritence - ensure using the same id overrides the defaults
     */
    public void testit0043()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0043" );

        File child1 = new File( testDir, "child1" );
        Verifier verifier = new Verifier( child1.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-help-plugin", "2.0.2", "jar" );

        verifier.executeGoal( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File child2 = new File( testDir, "child2" );
        verifier = new Verifier( child2.getAbsolutePath() );

        verifier.executeGoal( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

