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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3805">MNG-3805</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3805ExtensionClassPathOrderingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3805ExtensionClassPathOrderingTest()
    {
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,)" );
    }

    /**
     * Verify that the extension manager respects the ordering of the extension's dependencies when setting up the
     * class realm.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3805()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3805" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3805" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties wclProps = verifier.loadProperties( "target/wcl.properties" );
        String prefix = "org/apache/maven/its/mng3805/a.properties.";
        String resource = "org/apache/maven/its/mng3805/a.properties";
        assertEquals( "5", wclProps.getProperty( prefix + "count" ) );
        assertTrue( wclProps.getProperty( prefix + "0" ).endsWith( "wagon-a-0.1.jar!/" + resource ) );
        assertTrue( wclProps.getProperty( prefix + "1" ).endsWith( "dep-a-0.1.jar!/" + resource ) );
        assertTrue( wclProps.getProperty( prefix + "2" ).endsWith( "dep-b-0.1.jar!/" + resource ) );
        assertTrue( wclProps.getProperty( prefix + "3" ).endsWith( "dep-c-0.1.jar!/" + resource ) );
        assertTrue( wclProps.getProperty( prefix + "4" ).endsWith( "dep-d-0.1.jar!/" + resource ) );
    }

}
