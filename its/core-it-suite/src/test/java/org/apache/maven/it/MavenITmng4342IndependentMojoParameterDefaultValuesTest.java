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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4342">MNG-4342</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4342IndependentMojoParameterDefaultValuesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4342IndependentMojoParameterDefaultValuesTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that multiple goals within a single execution get their default configuration properly injected. In
     * particular, the default values for one goal should not influence the default values of the other goal.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4342" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props1 = verifier.loadProperties( "target/config1.properties" );
        assertEquals( "maven-core-it", props1.getProperty( "defaultParam" ) );

        Properties props2 = verifier.loadProperties( "target/config2.properties" );
        assertEquals( "test", props2.getProperty( "defaultParam" ) );
    }

}
