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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4536">MNG-4536</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4536RequiresNoProjectForkingMojoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4536RequiresNoProjectForkingMojoTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-alpha-7,)" );
    }

    /**
     * Test that forking mojos that require no project only fork the current project and not the entire reactor.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4536" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.executeGoal( "generate-sources" );
        verifier.verifyErrorFreeLog();

        assertEquals( 2, verifier.loadLines( "mod-a/target/touch.log", "UTF-8" ).size() );
        assertEquals( 2, verifier.loadLines( "mod-b/target/touch.log", "UTF-8" ).size() );
    }

}
