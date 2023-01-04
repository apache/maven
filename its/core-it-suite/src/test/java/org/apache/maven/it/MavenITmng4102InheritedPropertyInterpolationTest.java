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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4102">MNG-4102</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4102InheritedPropertyInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4102InheritedPropertyInterpolationTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that the effective value of an inherited property reflects the values of any nested property
     * as defined by the child. This boils down to the order of inheritance and (parent) interpolation.
     * This variation of the test has no profiles.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitNoProfiles()
        throws Exception
    {
        testit( "no-profile" );
    }

    /**
     * Verify that the effective value of an inherited property reflects the values of any nested property
     * as defined by the child. This boils down to the order of inheritance and (parent) interpolation.
     * This variation of the test has active profiles in parent and child (which should make no difference
     * to the result).
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitActiveProfiles()
        throws Exception
    {
        testit( "active-profile" );
    }

    private void testit( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4102/" + project );

        Verifier verifier = newVerifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "CHILD", props.getProperty( "project.properties.overridden" ) );
        assertEquals( "CHILD", props.getProperty( "project.properties.interpolated" ) );
    }

}
