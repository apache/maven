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

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2068">MNG-2068</a>.
 *
 * Verify that a multi-module build, built from the middle node in an inheritance hierarchy,
 * can find all parent POMs necessary to build each project in the reactor using ONLY the
 * relativePath from the parent specification (in this case, the implied one of '../pom.xml').
 *
 * @author jdcasey
 *
 */
public class MavenITmng2068ReactorRelativeParentsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2068ReactorRelativeParentsTest()
    {
        super( "(2.0.6,)" ); // only test in 2.0.7+
    }

    /**
     * Test successful lineage construction when parent inherits groupId+version from grand-parent.
     *
     * @throws Exception in case of failure
     */
    public void testitInheritedIdFields()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2068/test-1" );
        File projectDir = new File( testDir, "parent" );

        Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2068" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    /**
     * Test successful lineage construction when parent specifies groupId+version itself.
     *
     * @throws Exception in case of failure
     */
    public void testitExplicitIdFields()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2068/test-2" );
        File projectDir = new File( testDir, "parent" );

        Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2068" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    /**
     * Test that the implied relative path for the parent POM works, even two levels deep.
     *
     * @throws Exception in case of failure
     */
    public void testitComplex()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2068/test-3" );
        File projectDir = testDir;

        Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2068" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
