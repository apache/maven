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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4361">MNG-4361</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4361ForceDependencySnapshotUpdateTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4361ForceDependencySnapshotUpdateTest()
    {
        super( "[2.0,3.0-alpha-1),[3.0-alpha-4,)" );
    }

    /**
     * Verify that snapshot updates of dependencies can be forced from the command line via "-U". In more detail,
     * this means updating the JAR and its accompanying hierarchy of POMs.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4361" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4361" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );

        Properties filterProps = verifier.newDefaultFilterProperties();

        filterProps.setProperty( "@repo@", "repo-1" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-force-1.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertNull( verifier.loadProperties( "target/checksum.properties" ).getProperty( "b-0.1-SNAPSHOT.jar" ) );

        filterProps.setProperty( "@repo@", "repo-2" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-force-2.txt" );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "-U" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();


        Properties checksums = verifier.loadProperties( "target/checksum.properties" );
        assertChecksum( "2a22eeca91211193e927ea3b2ecdf56481585064", "a-0.1-SNAPSHOT.jar", checksums );
        assertChecksum( "ae352eb0047059b2e47fae397eb8ae6bd5b1c8ea", "b-0.1-SNAPSHOT.jar", checksums );
        assertChecksum( "6e6ef8590f166bcf610965c74c165128776214b9", "c-0.1-SNAPSHOT.jar", checksums );
    }

    private void assertChecksum( String checksum, String jar, Properties checksums )
    {
        assertEquals( checksum, checksums.getProperty( jar, "" ).toLowerCase( java.util.Locale.ENGLISH ) );
    }

}
