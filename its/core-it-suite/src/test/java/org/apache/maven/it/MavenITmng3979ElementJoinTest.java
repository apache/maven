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

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3979">MNG-3979</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3979ElementJoinTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3979ElementJoinTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that during inheritance the merging/joining of subtrees with equal identifier doesn't crash if the parent
     * POM has a non-empty element and the child POM has an empty element to join.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3979()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3979" );

        testDir = new File( testDir, "sub" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
