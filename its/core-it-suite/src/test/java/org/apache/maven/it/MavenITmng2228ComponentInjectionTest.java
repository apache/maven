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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2228">MNG-2228</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2228ComponentInjectionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2228ComponentInjectionTest()
    {
        super( "(2.0.4,)" );
    }

    /**
     * Verify that components injected into plugins are actually assignment-compatible with the corresponding mojo
     * fields in case the field type is both provided by a plugin dependency and by a build extension.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2228()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2228" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2228" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Properties apiProps = verifier.loadProperties( "target/api.properties" );
        assertEquals( "true", apiProps.getProperty( "org.apache.maven.its.mng2228.DefaultComponent" ) );
    }

}
