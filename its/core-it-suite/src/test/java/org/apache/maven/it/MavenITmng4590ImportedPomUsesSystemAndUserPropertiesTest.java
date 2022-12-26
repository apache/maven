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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4590">MNG-4590</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4590ImportedPomUsesSystemAndUserPropertiesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4590ImportedPomUsesSystemAndUserPropertiesTest()
    {
        super( "[2.0.9,3.0-alpha-1),[3.0-beta-1,)" );
    }

    /**
     * Verify that imported POMs are processed using the same system/user properties as the importing POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4590" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4590" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.setEnvironmentVariable( "MAVEN_OPTS", "-Dtest.file=pom.xml" );
        verifier.addCliArgument( "-Dtest.dir=" + testDir.getAbsolutePath() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "1", props.getProperty( "project.dependencyManagement.dependencies" ) );
        assertEquals( "dep-a", props.getProperty( "project.dependencyManagement.dependencies.0.artifactId" ) );
        assertEquals( new File( testDir, "pom.xml" ).getAbsoluteFile(),
            new File( props.getProperty( "project.dependencyManagement.dependencies.0.systemPath" ) ) );
    }

}
