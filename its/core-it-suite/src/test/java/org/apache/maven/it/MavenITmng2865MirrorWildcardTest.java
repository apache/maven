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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2865">MNG-2865</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2865MirrorWildcardTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2865MirrorWildcardTest()
    {
        super( "(2.0.4,)" );
    }

    /**
     * Test that the mirror wildcard * matches any repo, in particular file:// repos.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFileRepo()
        throws Exception
    {
        testit( "file" );
    }

    /**
     * Test that the mirror wildcard * matches any repo, in particular http://localhost repos.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitLocalhostRepo()
        throws Exception
    {
        testit( "localhost" );
    }

    /**
     * Test that the mirror wildcard * matches any repo, in particular external repos.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitExternalRepo()
        throws Exception
    {
        testit( "external" );
    }

    /**
     * Test that the mirror wildcard * matches any repo, in particular central.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCentralRepo()
        throws Exception
    {
        testit( "central" );
    }

    private void testit( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2865" );

        Verifier verifier = newVerifier( new File( testDir, project ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2865" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng2865", "a", "0.1", "jar" );
    }

}
