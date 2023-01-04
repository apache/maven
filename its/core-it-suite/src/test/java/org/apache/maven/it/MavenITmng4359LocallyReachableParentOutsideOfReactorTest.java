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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4359">MNG-4359</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4359LocallyReachableParentOutsideOfReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4359LocallyReachableParentOutsideOfReactorTest()
    {
        super( "[2.0.7,)" );
    }

    /**
     * Verify that locally reachable parent POMs of projects in the reactor can be resolved during dependency
     * resolution even if a parent itself is not part of the reactor.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4359" );
        testDir = new File( testDir, "reactor-parent" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "mod-c/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4359" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        List<String> cp = verifier.loadLines( "mod-c/target/classpath.txt", "UTF-8" );
        assertTrue( cp.toString(), cp.contains( "mod-b/pom.xml" ) );
        assertTrue( cp.toString(), cp.contains( "mod-a/pom.xml" ) );
    }

}
