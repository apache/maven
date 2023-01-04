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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MavenITmng7112ProjectsWithNonRecursiveTest
        extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7112-projects-with-non-recursive";

    public MavenITmng7112ProjectsWithNonRecursiveTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    @Test
    public void testAggregatesCanBeBuiltNonRecursively()
            throws IOException, VerificationException
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        Verifier cleaner = newVerifier( projectDir.getAbsolutePath() );
        cleaner.addCliArgument( "clean" );
        cleaner.execute();

        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( ":aggregator-a,:aggregator-b" );
        verifier.addCliArgument( "-N" );
        verifier.setLogFileName( "selected-non-recursive.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();

        verifier.verifyFileNotPresent( "target/touch.txt" );
        verifier.verifyFilePresent( "aggregator-a/target/touch.txt" );
        verifier.verifyFileNotPresent( "aggregator-a/module-a/target/touch.txt" );
        verifier.verifyFilePresent( "aggregator-b/target/touch.txt" );
        verifier.verifyFileNotPresent( "aggregator-b/module-b/target/touch.txt" );
    }

    @Test
    public void testAggregatesCanBeDeselectedNonRecursively()
            throws IOException, VerificationException
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        Verifier cleaner = newVerifier( projectDir.getAbsolutePath() );
        cleaner.addCliArgument( "clean" );
        cleaner.execute();

        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "!:aggregator-a,!:aggregator-b" );
        verifier.addCliArgument( "-N" );
        verifier.setLogFileName( "excluded-non-recursive.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "aggregator-a/target/touch.txt" );
        verifier.verifyFilePresent( "aggregator-a/module-a/target/touch.txt" );
        verifier.verifyFileNotPresent( "aggregator-b/target/touch.txt" );
        verifier.verifyFilePresent( "aggregator-b/module-b/target/touch.txt" );
    }
}
