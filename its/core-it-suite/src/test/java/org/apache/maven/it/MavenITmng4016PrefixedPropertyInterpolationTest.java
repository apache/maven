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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4016">MNG-4016</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4016PrefixedPropertyInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4016PrefixedPropertyInterpolationTest()
    {
        super( "(2.1.0-M1,)" );
    }

    /**
     * Test that expressions with the special prefixes "project.", "pom." and "env." can be interpolated from
     * properties that include the prefix.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG4016()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4016" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/model.properties" );
        Properties props = verifier.loadProperties( "target/model.properties" );
        assertEquals( "PASSED-1", props.getProperty( "project.properties.projectProperty" ) );
        assertEquals( "PASSED-2", props.getProperty( "project.properties.pomProperty" ) );
        assertEquals( "PASSED-3", props.getProperty( "project.properties.envProperty" ) );
    }

}
