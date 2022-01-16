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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3846">MNG-3846</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3846PomInheritanceUrlAdjustmentTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3846PomInheritanceUrlAdjustmentTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that inheritance of certain URLs automatically appends the child's artifact id.
     *
     * @throws Exception in case of failure
     */
    public void testitOneParent()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3846" );

        Verifier verifier = newVerifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "http://parent.url/child", props.getProperty( "project.url" ) );
        assertEquals( "http://parent.url/org/", props.getProperty( "project.organization.url" ) );
        assertEquals( "http://parent.url/license.txt", props.getProperty( "project.licenses.0.url" ) );
        assertEquals( "http://parent.url/viewvc/child", props.getProperty( "project.scm.url" ) );
        assertEquals( "http://parent.url/scm/child", props.getProperty( "project.scm.connection" ) );
        assertEquals( "https://parent.url/scm/child", props.getProperty( "project.scm.developerConnection" ) );
        assertEquals( "http://parent.url/issues", props.getProperty( "project.issueManagement.url" ) );
        assertEquals( "http://parent.url/ci", props.getProperty( "project.ciManagement.url" ) );
        assertEquals( "http://parent.url/dist", props.getProperty( "project.distributionManagement.repository.url" ) );
        assertEquals( "http://parent.url/snaps", props.getProperty( "project.distributionManagement.snapshotRepository.url" ) );
        assertEquals( "http://parent.url/site/child", props.getProperty( "project.distributionManagement.site.url" ) );
        assertEquals( "http://parent.url/download", props.getProperty( "project.distributionManagement.downloadUrl" ) );
    }

    /**
     * Test that inheritance of certain URLs automatically appends the child's artifact id. In a deeper inheritance
     * hierarchy, this should contribute the artifact id of each parent that does not override the URLs.
     *
     * @throws Exception in case of failure
     */
    public void testitTwoParents()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3846" );

        Verifier verifier = newVerifier( new File( testDir, "another-parent/sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "http://parent.url/ap/child", props.getProperty( "project.url" ) );
        assertEquals( "http://parent.url/org/", props.getProperty( "project.organization.url" ) );
        assertEquals( "http://parent.url/license.txt", props.getProperty( "project.licenses.0.url" ) );
        assertEquals( "http://parent.url/viewvc/ap/child", props.getProperty( "project.scm.url" ) );
        assertEquals( "http://parent.url/scm/ap/child", props.getProperty( "project.scm.connection" ) );
        assertEquals( "https://parent.url/scm/ap/child", props.getProperty( "project.scm.developerConnection" ) );
        assertEquals( "http://parent.url/issues", props.getProperty( "project.issueManagement.url" ) );
        assertEquals( "http://parent.url/ci", props.getProperty( "project.ciManagement.url" ) );
        assertEquals( "http://parent.url/dist", props.getProperty( "project.distributionManagement.repository.url" ) );
        assertEquals( "http://parent.url/snaps", props.getProperty( "project.distributionManagement.snapshotRepository.url" ) );
        assertEquals( "http://parent.url/site/ap/child", props.getProperty( "project.distributionManagement.site.url" ) );
        assertEquals( "http://parent.url/download", props.getProperty( "project.distributionManagement.downloadUrl" ) );
    }

}
