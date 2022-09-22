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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-598">MNG-598</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenIT0041ArtifactTypeFromPluginExtensionTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0041ArtifactTypeFromPluginExtensionTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test the use of a new type from a plugin
     *
     * @throws Exception in case of failure
     */
    public void testit0041()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0041" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven", "maven-core-it-support", "1.2" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.2", "coreit-artifact" );
        verifier.verifyArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.2", "pom" );
    }

}
