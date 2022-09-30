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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4116">MNG-4116</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4116UndecodedUrlsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4116UndecodedUrlsTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the project builder does not decode URLs (which must be done by the transport layer instead).
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4116()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4116" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        assertEquals( "http://maven.apache.org/spacy%20path",
            props.getProperty( "project.url" ) );

        assertEquals( "http://svn.apache.org/viewvc/spacy%20path",
            props.getProperty( "project.scm.url" ) );
        assertEquals( "scm:svn:svn+ssh://svn.apache.org/spacy%20path",
            props.getProperty( "project.scm.connection" ) );
        assertEquals( "scm:svn:svn+ssh://svn.apache.org/spacy%20path",
            props.getProperty( "project.scm.developerConnection" ) );

        assertEquals( "http://ci.apache.org/spacy%20path",
            props.getProperty( "project.ciManagement.url" ) );

        assertEquals( "http://issues.apache.org/spacy%20path",
            props.getProperty( "project.issueManagement.url" ) );

        assertEquals( "scm:svn:svn+ssh://dist.apache.org/spacy%20path",
            props.getProperty( "project.distributionManagement.repository.url" ) );
        assertEquals( "scm:svn:svn+ssh://snap.apache.org/spacy%20path",
            props.getProperty( "project.distributionManagement.snapshotRepository.url" ) );
        assertEquals( "scm:svn:svn+ssh://site.apache.org/spacy%20path",
            props.getProperty( "project.distributionManagement.site.url" ) );
    }

}
