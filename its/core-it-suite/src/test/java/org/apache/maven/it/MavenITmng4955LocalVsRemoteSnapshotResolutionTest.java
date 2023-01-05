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
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4955">MNG-4955</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4955LocalVsRemoteSnapshotResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4955LocalVsRemoteSnapshotResolutionTest()
    {
        super( "[2.0.10,2.0.99),[2.1.0,3.0-alpha-1),[3.0.2,)" );
    }

    /**
     * Verify that dependency resolution prefers newer local snapshots over outdated remote snapshots that use the new
     * metadata format.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4955" );

        Verifier verifier = newVerifier( new File( testDir, "dep" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4955" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );

        File jarFile = new File( classpath.get( 1 ).toString() );
        assertEquals( "eeff09b1b80e823eeb2a615b1d4b09e003e86fd3", ItUtils.calcHash( jarFile, "SHA-1" ) );
    }

}
