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
import org.apache.maven.it.util.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class MavenIT0047Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test the use case for having a compile time dependency be transitive:
     * when you extend a class you need its dependencies at compile time.
     */
    public void testit0047()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0047" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        Properties systemProperties = new Properties();
        systemProperties.put( "depres.compileClassPath", new File( testDir, "target/compile.txt" ).getAbsolutePath() );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution::compile" );
        verifier.assertFilePresent( "target/compile.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List lines = verifier.loadLines( "target/compile.txt", "UTF-8" );
        String paths = StringUtils.join( lines.iterator(), "\t" ).replace( '\\', '/' );
        assertTrue( paths.indexOf( "org/apache/maven/its/it0047/direct-dep/1.0/direct-dep-1.0.jar" ) >= 0 );
        assertTrue( paths.indexOf( "org/apache/maven/its/it0047/transitive-dep/1.1/transitive-dep-1.1.jar" ) >= 0 );
    }

}
