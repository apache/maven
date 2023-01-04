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
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4877">MNG-4877</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4877DeployUsingPrivateKeyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4877DeployUsingPrivateKeyTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0.1,)" );
    }

    /**
     * Verify that configured private key and passphrase are used for (SSH) deployment.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4877" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/wagon.properties" );
        assertEquals( "testuser", props.get( "username" ) );
        assertEquals( "testpass", props.get( "password" ) );
        assertEquals( "testkey", props.get( "privateKey" ) );
        assertEquals( "testphrase", props.get( "passphrase" ) );
    }

}
