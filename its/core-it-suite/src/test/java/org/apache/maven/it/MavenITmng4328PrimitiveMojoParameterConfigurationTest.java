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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4328">MNG-4328</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4328PrimitiveMojoParameterConfigurationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4328PrimitiveMojoParameterConfigurationTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that plugin parameters that are of primitive types like boolean (not java.lang.Boolean) can be populated
     * from expressions. In other words, the subtle difference between the runtime type of the expression value (which
     * will always be a primitive wrapper class due to reflection) and the actual parameter type should not matter.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4328" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "--offline" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props;

        props = verifier.loadProperties( "target/config1.properties" );
        assertEquals( "true", props.getProperty( "primitiveBooleanParam" ) );

        props = verifier.loadProperties( "target/config2.properties" );
        assertEquals( "true", props.getProperty( "primitiveBooleanParam" ) );
    }

}
