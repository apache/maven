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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-731">MNG-731</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0731LegacyRepoLayoutTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0731LegacyRepoLayoutTest()
    {
        // legacy layout no longer supported in Maven 3.x (see MNG-4204)
        super( "[2.0,3.0-alpha-3)" );
    }

    /**
     * Verify that deployment of artifacts to a legacy-layout repository
     * results in a groupId directory of 'the.full.group.id' instead of
     * 'the/full/group/id'.
     */
    public void testitMNG731()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0731" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.assertFilePresent( "target/test-repo/org.apache.maven.its.it0061/jars/maven-it-it0061-1.0.jar" );
        verifier.assertFilePresent( "target/test-repo/org.apache.maven.its.it0061/poms/maven-it-it0061-1.0.pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
