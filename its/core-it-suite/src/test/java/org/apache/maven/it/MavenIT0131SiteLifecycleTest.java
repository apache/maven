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
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0131SiteLifecycleTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0131SiteLifecycleTest()
    {
        super( "[2.0.0,)" );
    }

    /**
     * Test default binding of goals for "site" lifecycle.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0131()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0131" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoal( "site-deploy" );
        verifier.verifyFilePresent( "target/site-site.txt" );
        verifier.verifyFilePresent( "target/site-deploy.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
