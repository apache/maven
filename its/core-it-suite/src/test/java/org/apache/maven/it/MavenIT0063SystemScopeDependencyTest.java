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
import java.util.List;

public class MavenIT0063SystemScopeDependencyTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0063SystemScopeDependencyTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test the use of a system scoped dependency to a (fake) tools.jar.
     */
    public void testit0063()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0063" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.getSystemProperties().setProperty( "jre.home", new File( testDir, "jdk/jre" ).getPath() );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> lines = verifier.loadLines( "target/compile.txt", "UTF-8" );
        assertEquals( 2, lines.size() );
        assertEquals( new File( testDir, "jdk/lib/tools.jar").getCanonicalFile(),
                      new File( (String) lines.get(1) ).getCanonicalFile() );
    }

}
