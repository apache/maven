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

public class MavenIT0020Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test beanshell mojo support.
     */
    public void testit0020()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0020" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-it0020", "1.0-SNAPSHOT", "maven-plugin" );
        List goals = Arrays.asList( new String[]{"install"} );
        verifier.executeGoals( goals );

        verifier = new Verifier( testDir.getAbsolutePath() );        
        goals = Arrays.asList( new String[]{"org.apache.maven.its.it0020:maven-it-it0020:it0020"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/out.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}

