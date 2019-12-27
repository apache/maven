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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3864">MNG-3864</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3864PerExecPluginConfigTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3864PerExecPluginConfigTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that plain per-execution plugin configuration works correctly.
     */
    public void testitMNG3864()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3864" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/plugin-config.properties" );

        assertCanonicalFileEquals( new File( testDir, "pom.xml" ), new File( props.getProperty( "fileParam" ) ) );
        assertEquals( "true", props.getProperty( "booleanParam" ) );
        assertEquals( "42", props.getProperty( "byteParam" ) );
        assertEquals( "-12345", props.getProperty( "shortParam" ) );
        assertEquals( "0", props.getProperty( "integerParam" ) );
        assertEquals( "9876543210", props.getProperty( "longParam" ) );
        assertEquals( "0.0", props.getProperty( "floatParam" ) );
        assertEquals( "-1.5", props.getProperty( "doubleParam" ) );
        assertEquals( "X", props.getProperty( "characterParam" ) );
        assertEquals( "Hello World!", props.getProperty( "stringParam" ) );
        assertEquals( "2008-11-09 11:59:03", props.getProperty( "dateParam" ) );
        assertEquals( "http://maven.apache.org/", props.getProperty( "urlParam" ) );

        assertEquals( "4", props.getProperty( "stringParams" ) );
        assertEquals( "one", props.getProperty( "stringParams.0" ) );
        assertEquals( "two", props.getProperty( "stringParams.1" ) );
        assertEquals( "three", props.getProperty( "stringParams.2" ) );
        assertEquals( "four", props.getProperty( "stringParams.3" ) );

        assertEquals( "4", props.getProperty( "listParam" ) );
        assertEquals( "one", props.getProperty( "listParam.0" ) );
        assertEquals( "two", props.getProperty( "listParam.1" ) );
        assertEquals( "three", props.getProperty( "listParam.2" ) );
        assertEquals( "four", props.getProperty( "listParam.3" ) );

        assertEquals( "1", props.getProperty( "setParam" ) );
        assertEquals( "item", props.getProperty( "setParam.0" ) );

        assertEquals( "2", props.getProperty( "mapParam" ) );
        assertEquals( "value1", props.getProperty( "mapParam.key1" ) );
        assertEquals( "value2", props.getProperty( "mapParam.key2" ) );

        assertEquals( "2", props.getProperty( "propertiesParam" ) );
        assertEquals( "value1", props.getProperty( "propertiesParam.key1" ) );
        assertEquals( "value2", props.getProperty( "propertiesParam.key2" ) );

        assertEquals( "field", props.getProperty( "beanParam.fieldParam" ) );
        assertEquals( "setter", props.getProperty( "beanParam.setterParam" ) );
        assertEquals( "true", props.getProperty( "beanParam.setterCalled" ) );

        assertEquals( "4", props.getProperty( "domParam.children" ) );
        assertEquals( "one", props.getProperty( "domParam.children.echo.0.value" ) );
        assertEquals( "two", props.getProperty( "domParam.children.echo.1.value" ) );
        assertEquals( "three", props.getProperty( "domParam.children.echo.2.value" ) );
        assertEquals( "four", props.getProperty( "domParam.children.echo.3.value" ) );
    }

}
