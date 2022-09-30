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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4654">MNG-4654</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4654ArtifactHandlerForMainArtifactTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4654ArtifactHandlerForMainArtifactTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Test that the artifact handler for the project main artifact is selected via the handler's type/roleHint
     * and not via the handler's packaging (the packaging only applies to the legacy repo layout).
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4654" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4654" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFileNotPresent( "target/repo/org/apache/maven/its/mng4654/test/1.0/test-1.0.mng4654" );
        verifier.verifyFilePresent( "target/repo/org/apache/maven/its/mng4654/test/1.0/test-1.0.jar" );
    }

}
