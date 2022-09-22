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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4312">MNG-4312</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4312TypeAwarePluginParameterExpressionInjectionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4312TypeAwarePluginParameterExpressionInjectionTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verify that plugins that use magic parameter expressions like ${plugin} for ordinary system properties
     * get properly configured and don't crash due to Maven trying to inject a type-incompatible magic value
     * into the String-type mojo parameter.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4312" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/config.properties" );

        assertEquals( "", props.getProperty( "stringParam", "" ) );
        assertEquals( "", props.getProperty( "aliasParam", "" ) );
        assertEquals( "maven-core-it", props.getProperty( "defaultParam" ) );
    }

}
