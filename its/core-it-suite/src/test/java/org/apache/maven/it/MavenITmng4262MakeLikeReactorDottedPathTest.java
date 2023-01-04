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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4262">MNG-4262</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4262MakeLikeReactorDottedPathTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4262MakeLikeReactorDottedPathTest()
    {
        super( "[3.0-alpha-3,4.0.0-alpha-1)" );
    }

    private void clean( Verifier verifier )
        throws Exception
    {
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "../sub-a/target" );
    }

    /**
     * Verify that the project list can select the root project by its relative path ".".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMakeRoot()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4262" );

        Verifier verifier = newVerifier( new File( testDir, "parent" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "." );
        verifier.setLogFileName( "log-root.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "../sub-a/target/touch.txt" );
    }

    /**
     * Verify that the project list can select a submodule by a relative path like {@code "../<something>"}.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMakeModule()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4262" );

        Verifier verifier = newVerifier( new File( testDir, "parent" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "../sub-a" );
        verifier.setLogFileName( "log-module.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent( "target/touch.txt" );
        verifier.verifyFilePresent( "../sub-a/target/touch.txt" );
    }

}
