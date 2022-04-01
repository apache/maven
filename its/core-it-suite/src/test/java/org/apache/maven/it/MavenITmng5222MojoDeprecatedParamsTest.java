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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test for <a href="https://issues.apache.org/jira/browse/MNG-5222">MNG-5222</a>
 */
public class MavenITmng5222MojoDeprecatedParamsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5222MojoDeprecatedParamsTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    /**
     * Test that ensures that deprecation is not printed for empty and default value
     *
     * @throws Exception in case of failure
     */
    public void testEmptyConfiguration()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5222-mojo-deprecated-params" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log-empty-configuration.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> logLines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        List<String> warnLines = findDeprecationWarning( logLines );
        assertTrue( "Log contains warnings: " + warnLines, warnLines.isEmpty() );

        Properties configProps = verifier.loadProperties( "target/config.properties" );

        assertEquals( "0", configProps.remove( "deprecatedArray" ) );

        assertEquals( "3", configProps.remove( "deprecatedArrayWithDefaults" ) );
        assertEquals( "a1 ", configProps.remove( "deprecatedArrayWithDefaults.0" ) );
        assertEquals( "a2", configProps.remove( "deprecatedArrayWithDefaults.1" ) );
        assertEquals( " a3", configProps.remove( "deprecatedArrayWithDefaults.2" ) );

        assertEquals( "0", configProps.remove( "deprecatedList" ) );

        assertEquals( "3", configProps.remove( "deprecatedListWithDefaults" ) );
        assertEquals( "l1", configProps.remove( "deprecatedListWithDefaults.0" ) );
        assertEquals( "l2", configProps.remove( "deprecatedListWithDefaults.1" ) );
        assertEquals( "l3", configProps.remove( "deprecatedListWithDefaults.2" ) );

        assertEquals( "testValue", configProps.remove( "deprecatedParamWithDefaultConstant" ) );
        assertEquals( "https://www.test.org", configProps.remove( "deprecatedParamWithDefaultEvaluate" ) );

        assertTrue( "not checked config properties: " + configProps, configProps.isEmpty() );
    }

    /**
     * Test that ensures that deprecation is printed for deprecated parameter set by property
     *
     * @throws Exception in case of failure
     */
    public void testDeprecatedProperty()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5222-mojo-deprecated-params" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-Dconfig.deprecatedParam2=deprecatedValueInProps" );
        verifier.addCliOption( "-Dconfig.deprecatedArray=3,2,4,deprecated" );
        verifier.addCliOption( "-Dconfig.deprecatedList=4,5,deprecated" );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log-deprecated-property.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> logLines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        List<String> warnLines = findDeprecationWarning( logLines );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedParam2' (user property 'config.deprecatedParam2') is deprecated: No reason given" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedArray' (user property 'config.deprecatedArray') is deprecated: deprecated array" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedList' (user property 'config.deprecatedList') is deprecated: deprecated list" ) );

        assertTrue( "Not verified line: " + warnLines, warnLines.isEmpty() );

        Properties configProps = verifier.loadProperties( "target/config.properties" );

        assertEquals( "deprecatedValueInProps", configProps.remove( "deprecatedParam2" ) );

        assertEquals( "4", configProps.remove( "deprecatedArray" ) );
        assertEquals( "3", configProps.remove( "deprecatedArray.0" ) );
        assertEquals( "2", configProps.remove( "deprecatedArray.1" ) );
        assertEquals( "4", configProps.remove( "deprecatedArray.2" ) );
        assertEquals( "deprecated", configProps.remove( "deprecatedArray.3" ) );

        assertEquals( "3", configProps.remove( "deprecatedArrayWithDefaults" ) );
        assertEquals( "a1 ", configProps.remove( "deprecatedArrayWithDefaults.0" ) );
        assertEquals( "a2", configProps.remove( "deprecatedArrayWithDefaults.1" ) );
        assertEquals( " a3", configProps.remove( "deprecatedArrayWithDefaults.2" ) );

        assertEquals( "3", configProps.remove( "deprecatedList" ) );
        assertEquals( "4", configProps.remove( "deprecatedList.0" ) );
        assertEquals( "5", configProps.remove( "deprecatedList.1" ) );
        assertEquals( "deprecated", configProps.remove( "deprecatedList.2" ) );

        assertEquals( "3", configProps.remove( "deprecatedListWithDefaults" ) );
        assertEquals( "l1", configProps.remove( "deprecatedListWithDefaults.0" ) );
        assertEquals( "l2", configProps.remove( "deprecatedListWithDefaults.1" ) );
        assertEquals( "l3", configProps.remove( "deprecatedListWithDefaults.2" ) );

        assertEquals( "testValue", configProps.remove( "deprecatedParamWithDefaultConstant" ) );
        assertEquals( "https://www.test.org", configProps.remove( "deprecatedParamWithDefaultEvaluate" ) );

        assertTrue( "not checked config properties: " + configProps, configProps.isEmpty() );
    }

    /**
     * Test that ensures that deprecation is printed for deprecated parameter set by plugin configuration.
     *
     * @throws Exception in case of failure
     */
    public void testDeprecatedConfig()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5222-mojo-deprecated-params" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-Pconfig-values" );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log-deprecated-config.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> logLines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        List<String> warnLines = findDeprecationWarning( logLines );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedParam' is deprecated: I'm deprecated param" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedParam2' (user property 'config.deprecatedParam2') is deprecated: No reason given" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedParamWithDefaultConstant' is deprecated: deprecated with constant value" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedParamWithDefaultEvaluate' is deprecated: deprecated with evaluate value" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedArray' (user property 'config.deprecatedArray') is deprecated: deprecated array" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedArrayWithDefaults' is deprecated: deprecated array" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedList' (user property 'config.deprecatedList') is deprecated: deprecated list" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedListWithDefaults' is deprecated: deprecated list" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedProperties' is deprecated: deprecated properties" ) );

        assertTrue( warnLines.remove(
            "[WARNING]   Parameter 'deprecatedMap' is deprecated: deprecated map" ));

        assertTrue( "Not verified line: " + warnLines, warnLines.isEmpty() );

        Properties configProps = verifier.loadProperties( "target/config.properties" );

        assertEquals( "value1", configProps.remove( "deprecatedParam" ) );
        assertEquals( "value2", configProps.remove( "deprecatedParam2" ) );
        assertEquals( "value3", configProps.remove( "deprecatedParamWithDefaultConstant" ) );
        assertEquals( "value4", configProps.remove( "deprecatedParamWithDefaultEvaluate" ) );

        assertEquals( "2", configProps.remove( "deprecatedArray" ) );
        assertEquals( "a1", configProps.remove( "deprecatedArray.0" ) );
        assertEquals( "a2", configProps.remove( "deprecatedArray.1" ) );

        assertEquals( "2", configProps.remove( "deprecatedArrayWithDefaults" ) );
        assertEquals( "b1", configProps.remove( "deprecatedArrayWithDefaults.0" ) );
        assertEquals( "b2", configProps.remove( "deprecatedArrayWithDefaults.1" ) );

        assertEquals( "2", configProps.remove( "deprecatedList" ) );
        assertEquals( "c1", configProps.remove( "deprecatedList.0" ) );
        assertEquals( "c2", configProps.remove( "deprecatedList.1" ) );

        assertEquals( "2", configProps.remove( "deprecatedListWithDefaults" ) );
        assertEquals( "d1", configProps.remove( "deprecatedListWithDefaults.0" ) );
        assertEquals( "d2", configProps.remove( "deprecatedListWithDefaults.1" ) );

        assertEquals( "2", configProps.remove( "deprecatedProperties" ) );
        assertEquals( "propertyValue1", configProps.remove( "deprecatedProperties.propertyName1" ) );
        assertEquals( "propertyValue2", configProps.remove( "deprecatedProperties.propertyName2" ) );

        assertEquals( "2", configProps.remove( "deprecatedMap" ) );
        assertEquals( "value1", configProps.remove( "deprecatedMap.key1" ) );
        assertEquals( "value2", configProps.remove( "deprecatedMap.key2" ) );

        assertTrue( "not checked config properties: " + configProps, configProps.isEmpty() );
    }

    private List<String> findDeprecationWarning( List<String> logLines )
    {
        Pattern pattern = Pattern.compile( "\\[WARNING] {3}Parameter .* is deprecated:.*" );
        List<String> result = new ArrayList<>();
        for ( String line : logLines )
        {
            if ( pattern.matcher( line ).matches() )
            {
                result.add( line );
            }
        }
        return result;
    }
}
