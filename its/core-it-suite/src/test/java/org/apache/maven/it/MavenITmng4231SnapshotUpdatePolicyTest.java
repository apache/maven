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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4231">MNG-4231</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4231SnapshotUpdatePolicyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4231SnapshotUpdatePolicyTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test the update policy "always" for snapshot dependencies is respected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitAlways()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4231" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4231" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.setProperty( "@updates@", "always" );

        filterProps.setProperty( "@repo@", "repo-1" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-always-1.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        filterProps.setProperty( "@repo@", "repo-2" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-always-2.txt" );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.resetStreams();

        Properties checksums = verifier.loadProperties( "target/checksum.properties" );
        assertChecksum( "db3f17644e813af768ae6e82a6d0a2f29aef8988", "a-0.1-SNAPSHOT.jar", checksums );
        assertChecksum( "5e3265f3ed55e8b217ff9db444fd8d888962a990", "b-0.1-SNAPSHOT.jar", checksums );
    }

    /**
     * Test the update policy "never" for snapshot dependencies is respected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitNever()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4231" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4231" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.setProperty( "@updates@", "never" );

        filterProps.setProperty( "@repo@", "repo-1" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-never-1.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        filterProps.setProperty( "@repo@", "repo-2" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-never-2.txt" );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.resetStreams();

        Properties checksums = verifier.loadProperties( "target/checksum.properties" );
        assertChecksum( "ec6c9ea65766cc272df0ee26076240d6a93047d5", "a-0.1-SNAPSHOT.jar", checksums );
        assertChecksum( "", "b-0.1-SNAPSHOT.jar", checksums );
    }

    private void assertChecksum( String checksum, String jar, Properties checksums )
    {
        assertEquals( checksum, checksums.getProperty( jar, "" ).toLowerCase( java.util.Locale.ENGLISH ) );
    }

}
