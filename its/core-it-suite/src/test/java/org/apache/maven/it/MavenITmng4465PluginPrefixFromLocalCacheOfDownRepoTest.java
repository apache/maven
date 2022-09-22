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

import java.io.File;
import java.util.Collections;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4465">MNG-4465</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4465PluginPrefixFromLocalCacheOfDownRepoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4465PluginPrefixFromLocalCacheOfDownRepoTest()
    {
        super( "[2.1.0,3.0-alpha-1),[3.0-alpha-6,)" );
    }

    /**
     * Verify that locally cached metadata of non-accessible remote repos is still considered when resolving
     * plugin prefixes.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4465" );

        // phase 1: get the metadata into the local repo

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4465" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.setLogFileName( "log1.txt" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "mng4465:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/touch.properties" );

        // phase 2: re-try with the remote repo being inaccessible (due to bad URL)

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8",
            Collections.singletonMap( "@baseurl@", "bad://localhost:63412" ) );
        verifier.setLogFileName( "log2.txt" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "mng4465:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/touch.properties" );
    }

}
