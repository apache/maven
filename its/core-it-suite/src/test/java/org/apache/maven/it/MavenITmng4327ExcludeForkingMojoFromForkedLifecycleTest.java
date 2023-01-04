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
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4327">MNG-4327</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4327ExcludeForkingMojoFromForkedLifecycleTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4327ExcludeForkingMojoFromForkedLifecycleTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that lifecycle forking mojos are excluded from the lifecycles that have directly or indirectly forked
     * by them.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4327" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "generate-sources" );
        verifier.verifyErrorFreeLog();

        List<String> log = verifier.loadLines( "target/fork-lifecycle.txt", "UTF-8" );
        assertEquals( 1, log.size() );
        assertTrue( log.toString(), log.contains( "fork-lifecycle.txt" ) );
    }

}
