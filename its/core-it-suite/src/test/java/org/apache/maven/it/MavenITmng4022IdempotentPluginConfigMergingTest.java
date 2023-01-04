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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4022">MNG-4022</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4022IdempotentPluginConfigMergingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4022IdempotentPluginConfigMergingTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Test that merging of equal plugin configuration is idempotent. This is especially interesting for lists with
     * empty elements.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4022" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "-Pmng4022a,mng4022b" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/config.properties" );
        assertEquals( "5", props.getProperty( "stringParams" ) );
        assertEquals( "", props.getProperty( "stringParams.0", "" ) );
        assertEquals( "one", props.getProperty( "stringParams.1", "" ) );
        assertEquals( "", props.getProperty( "stringParams.2", "" ) );
        assertEquals( "two", props.getProperty( "stringParams.3", "" ) );
        assertEquals( "", props.getProperty( "stringParams.4", "" ) );
    }

}
