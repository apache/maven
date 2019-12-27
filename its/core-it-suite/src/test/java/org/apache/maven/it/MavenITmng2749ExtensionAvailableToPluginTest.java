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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2749">MNG-2749</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2749ExtensionAvailableToPluginTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2749ExtensionAvailableToPluginTest()
    {
        super( "(2.0.2,)" );
    }

    /**
     * Verify that plugins can load classes/resources from a build extension.
     */
    public void testitMNG2749()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2749" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2749" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps = verifier.loadProperties( "target/pcl.properties" );
        assertNotNull( pclProps.getProperty( "org.apache.maven.its.mng2749.ExtensionClass" ) );
        assertNotNull( pclProps.getProperty( "org/apache/maven/its/mng2749/extension.properties" ) );

        Properties tcclProps = verifier.loadProperties( "target/tccl.properties" );
        assertEquals( pclProps, tcclProps );
    }

}
