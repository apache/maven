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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3807">MNG-3807</a>.
 *
 *
 */
public class MavenITmng3807PluginConfigExpressionEvaluationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3807PluginConfigExpressionEvaluationTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verify that plugin configurations are subject to the parameter expression evaluator, in particular composite
     * parameter types.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3807" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/config.properties" );

        assertEvaluated( props.getProperty( "stringParams.0" ) );
        assertEvaluated( props.getProperty( "stringParams.1" ) );

        assertEvaluated( props.getProperty( "listParam.0" ) );
        assertEvaluated( props.getProperty( "listParam.1" ) );

        assertEvaluated( props.getProperty( "mapParam.test0" ) );
        assertEvaluated( props.getProperty( "mapParam.test1" ) );

        assertEvaluated( props.getProperty( "propertiesParam.test0" ) );
        assertEvaluated( props.getProperty( "propertiesParam.test1" ) );
    }

    private void assertEvaluated( String value )
    {
        assertNotNull( value );
        assertTrue( value.length() > 0 );
        assertFalse( value, value.contains( "${" ) );
    }

}
