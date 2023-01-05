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
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4367">MNG-4367</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4367LayoutAwareMirrorSelectionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4367LayoutAwareMirrorSelectionTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Test that mirror selection considers the repo layout if specified for the mirror. If {@code <mirrorOfLayouts>} is
     * unspecified, should match any layout.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitNoLayout()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4367" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4367" );

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@repourl@", filterProps.get( "@baseurl@" ) + "/void" );
        filterProps.put( "@mirrorurl@", filterProps.get( "@baseurl@" ) + "/repo" );
        filterProps.put( "@layouts@", "" );

        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings-a.xml" );
        verifier.filterFile( "settings-template.xml", "settings-a.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-a.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4367", "dep", "0.1", "jar" );
    }

    /**
     * Test that mirror selection considers the repo layout if specified for the mirror.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitSpecificLayouts()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4367" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4367" );

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@repourl@", filterProps.get( "@baseurl@" ) + "/void" );
        filterProps.put( "@mirrorurl@", filterProps.get( "@baseurl@" ) + "/repo" );
        filterProps.put( "@layouts@", "default,legacy" );

        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings-b.xml" );
        verifier.filterFile( "settings-template.xml", "settings-b.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-b.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4367", "dep", "0.1", "jar" );
    }

    /**
     * Test that mirror selection considers the repo layout if specified for the mirror.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitNonMatchingLayout()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4367" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4367" );

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@repourl@", filterProps.get( "@baseurl@" ) + "/repo" );
        filterProps.put( "@mirrorurl@", filterProps.get( "@baseurl@" ) + "/void" );
        filterProps.put( "@layouts@", "foo" );

        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings-c.xml" );
        verifier.filterFile( "settings-template.xml", "settings-c.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-c.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4367", "dep", "0.1", "jar" );
    }

}
