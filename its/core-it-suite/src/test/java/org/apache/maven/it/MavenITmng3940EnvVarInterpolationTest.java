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

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.Os;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3940">MNG-3940</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3940EnvVarInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3940EnvVarInterpolationTest()
    {
        super( "(2.0.10,2.1.0-M1),(2.1.0-M1,)" );
    }

    /**
     * Test that interpolation of environment variables respects the casing rules of the underlying OS (especially
     * Windows).
     */
    public void testitMNG3940()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3940" );

        Map<String, String> envVars = new HashMap<>();
        /*
         * NOTE: The POM is using MAVEN_MNG_3940 to reference the var (just as one would refer to PATH). On Windows,
         * this must resolve case-insensitively so we use different character casing for the variable here.
         */
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            envVars.put( "Maven_mng_3940", "PASSED" );
        }
        else
        {
            envVars.put( "MAVEN_MNG_3940", "PASSED" );
        }

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate", envVars );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/PASSED.properties" );
        Properties props = verifier.loadProperties( "target/PASSED.properties" );
        assertEquals( "PASSED", props.getProperty( "project.properties.envTest" ) );
    }

}
