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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4474">MNG-4474</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4474PerLookupWagonInstantiationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4474PerLookupWagonInstantiationTest()
    {
        super( "[2.0.5,3.0-alpha-1),[3.0-alpha-6,)" );
    }

    /**
     * Verify that the wagon manager does not erroneously cache/reuse wagon instances that use per-lookup instantiation.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4474" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/wagon.properties" );
        String hash1 = props.getProperty( "coreit://one.hash" );
        assertNotNull( hash1 );
        String hash2 = props.getProperty( "coreit://two.hash" );
        assertNotNull( hash2 );
        assertNotEquals( hash1, hash2 );
    }

}
