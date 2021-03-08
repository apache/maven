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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;

public class MavenITmng7112ProjectsWithNonRecursiveTest
        extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7112-projects-with-non-recursive";

    public MavenITmng7112ProjectsWithNonRecursiveTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    public void testAggregatesCanBeBuiltNonRecursively()
            throws IOException, VerificationException
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        newVerifier( projectDir.getAbsolutePath() ).executeGoal( "clean" );

        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-pl" );
        verifier.addCliOption( ":aggregator-a,:aggregator-b" );
        verifier.addCliOption( "-N" );
        verifier.setLogFileName( "selected-non-recursive.txt" );
        verifier.executeGoal( "validate" );

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFilePresent( "aggregator-a/target/touch.txt" );
        verifier.assertFileNotPresent( "aggregator-a/module-a/target/touch.txt" );
        verifier.assertFilePresent( "aggregator-b/target/touch.txt" );
        verifier.assertFileNotPresent( "aggregator-b/module-b/target/touch.txt" );
    }

    public void testAggregatesCanBeDeselectedNonRecursively()
            throws IOException, VerificationException
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        newVerifier( projectDir.getAbsolutePath() ).executeGoal( "clean" );

        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!:aggregator-a,!:aggregator-b" );
        verifier.addCliOption( "-N" );
        verifier.setLogFileName( "excluded-non-recursive.txt" );
        verifier.executeGoal( "validate" );

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "aggregator-a/target/touch.txt" );
        verifier.assertFilePresent( "aggregator-a/module-a/target/touch.txt" );
        verifier.assertFileNotPresent( "aggregator-b/target/touch.txt" );
        verifier.assertFilePresent( "aggregator-b/module-b/target/touch.txt" );
    }
}
