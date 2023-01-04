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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3944">MNG-3944</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3944BasedirInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3944BasedirInterpolationTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that interpolation of ${basedir} works for a POM that is not named "pom.xml"
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3944()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3944" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-f" );
        verifier.addCliOption( "pom-with-unusual-name.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/basedir.properties" );
        Properties props = verifier.loadProperties( "target/basedir.properties" );
        assertCanonicalFileEquals( testDir, new File( props.getProperty( "project.properties.prop0" ) ) );
        assertCanonicalFileEquals( testDir, new File( props.getProperty( "project.properties.prop1" ) ) );
    }

}
