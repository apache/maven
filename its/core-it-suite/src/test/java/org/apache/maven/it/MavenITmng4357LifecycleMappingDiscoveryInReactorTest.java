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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4357">MNG-4357</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4357LifecycleMappingDiscoveryInReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4357LifecycleMappingDiscoveryInReactorTest()
    {
        super( "[2.1.0,)" );
    }

    /**
     * Test that different projects in a reactor build can use different versions of the same extension.
     * This should still hold true if the two versions of the extension provide the same set of components.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4357" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4357" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "mod-a/target/old.txt" );
        verifier.verifyFileNotPresent( "mod-a/target/new.txt" );

        verifier.verifyFileNotPresent( "mod-b/target/old.txt" );
        verifier.verifyFilePresent( "mod-b/target/new.txt" );
    }

}
