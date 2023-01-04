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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4408">MNG-4408</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4408NonExistentSettingsFileTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4408NonExistentSettingsFileTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verify that the build fails when the user specifies a non-existing user settings file on the CLI.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitUserSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4408" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-user.txt" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "non-existing-settings.xml" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Missing settings file did not cause build error" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
    }

    /**
     * Verify that the build fails when the user specifies a non-existing global settings file on the CLI.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitGlobalSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4408" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-global.txt" );
        verifier.addCliOption( "--global-settings" );
        verifier.addCliOption( "non-existing-settings.xml" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Missing settings file did not cause build error" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
    }

}
