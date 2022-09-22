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
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

public class MavenITmng7335MissingJarInParallelBuild
        extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7335-missing-jar-in-parallel-build";

    public MavenITmng7335MissingJarInParallelBuild()
    {
        super( "[3.8.1,)" );
    }

    public void testMissingJarInParallelBuild() throws IOException, VerificationException
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.addCliOption( "-T1C" );
        verifier.executeGoals( Arrays.asList( "clean", "package" ) );
        verifier.verifyErrorFreeLog();
    }
}
