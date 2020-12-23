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
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3863">MNG-3863</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3863AutoPluginGroupIdTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3863AutoPluginGroupIdTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that the group id "org.apache.maven.plugins" is *not* automatically assumed for dependencies.
     */
    public void testitMNG3853()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3863" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Validation step did not detect missing groupId for dependency" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
        verifier.resetStreams();
    }

}
