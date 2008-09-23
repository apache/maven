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

public class MavenIT0003Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Builds upon it0001: we add a jar installation step. We delete the JAR
     * from the local repository to make sure it is there post build.
     */
    public void testit0003()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0003" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-it-it0003", "1.0", "jar" );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0003/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0003/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0003-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0003-1.0.jar!/it0003.properties" );
        verifier.assertArtifactPresent( "org.apache.maven.its.it0003", "maven-it-it0003", "1.0", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

