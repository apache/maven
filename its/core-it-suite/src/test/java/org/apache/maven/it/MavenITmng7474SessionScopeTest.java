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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7474">MNG-7474</a>:
 * check that Session scope beans are actually singletons for the session.
 */
public class MavenITmng7474SessionScopeTest
        extends AbstractMavenIntegrationTestCase
{
    public MavenITmng7474SessionScopeTest()
    {
        super( "[3.9.0,)" );
    }

    @Test
    public void testSessionScope()
            throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7474-session-scope" );

        Verifier verifier = newVerifier( new File( testDir, "plugin" ).getAbsolutePath() );
        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( new File( testDir, "project" ).getAbsolutePath() );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

}
