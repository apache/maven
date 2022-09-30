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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-866">MNG-866</a> and
 * <a href="https://issues.apache.org/jira/browse/MNG-160">MNG-160</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0866EvaluateDefaultValueTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0866EvaluateDefaultValueTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that expressions inside the default value of plugin parameters are evaluated.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG866()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0866" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties configProps = verifier.loadProperties( "target/config.properties" );
        assertEquals( "maven-core-it", configProps.getProperty( "defaultParam" ) );
        assertEquals( "org.apache.maven.its.mng0866:test:1.0-SNAPSHOT", configProps.getProperty( "defaultParamWithExpression" ) );
    }

}
