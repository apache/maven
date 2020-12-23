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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1995">MNG-1995</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng1995InterpolateBooleanModelElementsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1995InterpolateBooleanModelElementsTest()
    {
        super( "[3.0-alpha-1,)" );
    }

    /**
     * Verify that POM fields that are of type boolean can be interpolated with expressions.
     */
    public void testitMNG1995()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1995" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/expression.properties" );
        assertEquals( "true", props.getProperty( "project.build.resources.0.filtering" ) );
        boolean foundTestRepo = false;
        for ( int i = Integer.parseInt( props.getProperty( "project.repositories" ) ) - 1; i >= 0; i-- )
        {
            if ( "maven-core-it".equals( props.getProperty( "project.repositories." + i + ".id" ) ) )
            {
                assertEquals( "true", props.getProperty( "project.repositories." + i + ".releases.enabled" ) );
                foundTestRepo = true;
            }
        }
        assertTrue( foundTestRepo );
    }

}
