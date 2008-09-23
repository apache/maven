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
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

public class MavenIT0031Test
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0031Test()
    {
    }            

    /**
     * Test usage of plugins.xml mapping file on the repository to resolve plugin artifactId from it's prefix using the
     * pluginGroups in the provided settings.xml.
     */
    public void testit0031()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0031" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--settings settings.xml" );
        verifier.setCliOptions( cliOptions );
        Properties systemProperties = new Properties();
        systemProperties.put( "model", "src/main/mdo/test.mdo" );
        systemProperties.put( "version", "1.0.0" );
        verifier.setSystemProperties( systemProperties );
        Properties verifierProperties = new Properties();
        verifierProperties.put( "failOnErrorOutput", "false" );
        verifier.setVerifierProperties( verifierProperties );
        verifier.executeGoal( "modello:java" );
        verifier.assertFilePresent( "target/generated-sources/modello/org/apache/maven/it/it0031/Root.java" );
        // don't verify error free log
        verifier.resetStreams();

    }
}
