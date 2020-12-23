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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4679">MNG-4679</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4679SnapshotUpdateInPluginTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4679SnapshotUpdateInPluginTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-2,)" );
    }

    /**
     * Verify that plugins using the 2.x style artifact resolver/collector directly are subject to the snapshot update
     * mode of the current Maven session.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4679" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4679" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        Properties filterProps = verifier.newDefaultFilterProperties();

        filterProps.setProperty( "@repo@", "repo-1" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-force-1.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        assertChecksum( verifier, "jar", "2ea5c3d713bbaba7b87746449b91cd00e876703d" );
        assertChecksum( verifier, "pom", "0b58dbbc61f81b85a70692ffdce88cf1892a8da4" );

        filterProps.setProperty( "@repo@", "repo-2" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-force-2.txt" );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-U" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.resetStreams();

        assertChecksum( verifier, "jar", "f3d46277c2ab45ff9bbd97605c942bed7fc27f97" );
        assertChecksum( verifier, "pom", "127f0dc26035352bb54890315ad7d2ada067756a" );
    }

    private void assertChecksum( Verifier verifier, String ext, String checksum )
        throws Exception
    {
        String path = verifier.getArtifactPath( "org.apache.maven.its.mng4679", "dep", "0.1-SNAPSHOT", ext );
        String actual = ItUtils.calcHash( new File( path ), "SHA-1" );
        assertEquals( checksum, actual );
    }

}
