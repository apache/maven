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
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0138PluginLifecycleTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0138PluginLifecycleTest()
    {
        super( "[2.0.0,)" );
    }

    /**
     * Test default binding of goals for "maven-plugin" lifecycle.
     */
    public void testit0138()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0138" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoal( "deploy" );
        verifier.assertFilePresent( "target/plugin-descriptor.txt" );
        verifier.assertFilePresent( "target/resources-resources.txt" );
        verifier.assertFilePresent( "target/compiler-compile.txt" );
        verifier.assertFilePresent( "target/resources-test-resources.txt" );
        verifier.assertFilePresent( "target/compiler-test-compile.txt" );
        verifier.assertFilePresent( "target/surefire-test.txt" );
        verifier.assertFilePresent( "target/jar-jar.txt" );
        verifier.assertFilePresent( "target/plugin-add-plugin-artifact-metadata.txt" );
        verifier.assertFilePresent( "target/install-install.txt" );
        if ( matchesVersionRange( "(,2.2.0)" ) )
        {
            verifier.assertFilePresent( "target/plugin-update-registry.txt" );
        }
        verifier.assertFilePresent( "target/deploy-deploy.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
