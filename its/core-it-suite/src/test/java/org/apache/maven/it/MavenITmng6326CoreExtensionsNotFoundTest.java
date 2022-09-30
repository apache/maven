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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6326">MNG-6326</a>:
 * check that Maven fails if it cannot load core extensions contributed by <code>.mvn/extensions.xml</code>.
 */
public class MavenITmng6326CoreExtensionsNotFoundTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6326CoreExtensionsNotFoundTest()
    {
        super( "[3.8.5,)" );
    }

    @Test
    public void testCoreExtensionsNotFound()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6326-core-extensions-not-found" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        try
        {
            verifier.executeGoal( "validate" );
            fail( "should have failed ");
        }
        catch ( VerificationException e )
        {
            try
            {
                verifier.verifyTextInLog( "[ERROR] Error executing Maven." );
                verifier.verifyTextInLog( "Extension org.apache.maven.its.it-core-extensions:maven-it-unknown-extensions:0.1 or one of its dependencies could not be resolved" );
            }
            catch ( VerificationException e2 )
            {
                throw new VerificationException( e2.getMessage() + "\nLog:" + getLogContent( verifier ) );
            }
        }
    }

    private String getLogContent( Verifier verifier ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy( Paths.get( verifier.getBasedir(), verifier.getLogFileName() ), baos );
        return baos.toString();
    }

}
