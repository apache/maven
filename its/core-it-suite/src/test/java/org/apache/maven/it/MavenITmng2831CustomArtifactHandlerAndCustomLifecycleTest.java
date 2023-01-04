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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2831">MNG-2831</a>.
 *
 *
 */
public class MavenITmng2831CustomArtifactHandlerAndCustomLifecycleTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2831CustomArtifactHandlerAndCustomLifecycleTest()
    {
        super( "(2.0.1,2.0.5),(2.0.6,3.0-alpha-1),[3.0-alpha-3,)" );
    }

    /**
     * Test the use of a custom lifecycle from a plugin that is defined as a build extension.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2831()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2831" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/test-1.0-it.jar" );
    }

}
