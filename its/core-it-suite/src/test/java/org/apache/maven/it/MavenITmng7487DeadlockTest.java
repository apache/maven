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
package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MavenITmng7487DeadlockTest extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7487-deadlock";

    public MavenITmng7487DeadlockTest()
    {
        super( "(,3.8.4],[3.8.6,)" );
    }

    @Test
    public void testDeadlock() throws IOException, VerificationException
    {
        final File rootDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );

        final File pluginDir = new File( rootDir, "plugin" );
        final Verifier pluginVerifier = newVerifier( pluginDir.getAbsolutePath() );
        pluginVerifier.addCliArgument( "install" );
        pluginVerifier.execute();

        final File consumerDir = new File( rootDir, "consumer" );
        final Verifier consumerVerifier = newVerifier( consumerDir.getAbsolutePath() );
        consumerVerifier.setForkJvm( true );;
        consumerVerifier.addCliOption( "-T2" );
        consumerVerifier.addCliArgument( "package" );
        consumerVerifier.execute();
        consumerVerifier.verifyErrorFreeLog();
        consumerVerifier.verifyTextInLog( "BUILD SUCCESS" );
    }

}
