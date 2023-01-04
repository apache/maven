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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4383">MNG-4383</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4383ValidDependencyVersionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4383ValidDependencyVersionTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Test that non-interpolated dependency versions cause a validation error during building.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4383" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail( "Invalid dependency version did not cause validation error" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
    }

}
