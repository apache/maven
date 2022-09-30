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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3845">MNG-3845</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3845LimitedPomInheritanceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3845LimitedPomInheritanceTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that inheritance is all-or-nothing for certain subtrees of the POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3845()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3845" );

        Verifier verifier = newVerifier( new File( testDir, "child" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "child-org", props.getProperty( "project.organization.name", "" ) );
        assertEquals( "", props.getProperty( "project.organization.url", "" ) );
        assertEquals( "http://child.url/issues", props.getProperty( "project.issueManagement.url", "" ) );
        assertEquals( "", props.getProperty( "project.issueManagement.system", "" ) );
        assertEquals( "0", props.getProperty( "project.ciManagement.notifiers", "0" ) );
        assertEquals( "child-distros", props.getProperty( "project.distributionManagement.repository.id", "" ) );
        assertEquals( "ssh://child.url/distros", props.getProperty( "project.distributionManagement.repository.url", "" ) );
        assertEquals( "", props.getProperty( "project.distributionManagement.repository.name", "" ) );
        assertEquals( "true", props.getProperty( "project.distributionManagement.repository.uniqueVersion", "true" ) );
        assertEquals( "default", props.getProperty( "project.distributionManagement.repository.layout", "default" ) );
        assertEquals( "child-snaps", props.getProperty( "project.distributionManagement.snapshotRepository.id", "" ) );
        assertEquals( "ssh://child.url/snaps", props.getProperty( "project.distributionManagement.snapshotRepository.url", "" ) );
        assertEquals( "", props.getProperty( "project.distributionManagement.snapshotRepository.name", "" ) );
        assertEquals( "true", props.getProperty( "project.distributionManagement.snapshotRepository.uniqueVersion", "true" ) );
        assertEquals( "default", props.getProperty( "project.distributionManagement.snapshotRepository.layout", "default" ) );
        assertEquals( "child-site", props.getProperty( "project.distributionManagement.site.id", "" ) );
        assertEquals( "scp://child.url/site", props.getProperty( "project.distributionManagement.site.url", "" ) );
        assertEquals( "", props.getProperty( "project.distributionManagement.site.name", "" ) );
    }

}
