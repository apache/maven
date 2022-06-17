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

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3819">MNG-3819</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3819PluginDepPlexusUtilsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3819PluginDepPlexusUtilsTest()
    {
        super( "(2.0.1,3.9.0)" );
    }

    /**
     * Verify that plexus-utils:1.1 is present on plugin class path if plexus-utils is not explicitly declared in
     * plugin POM for backward-compat with Maven 2.0.5- (due to MNG-2892, plexus-utils is no longer part of the core
     * class realm in Maven 2.0.6+).
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3819()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3819" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps = verifier.loadProperties( "target/pcl.properties" );

        assertNotNull( pclProps.getProperty( "org.codehaus.plexus.util.cli.CommandLineUtils" ) );
        assertEquals( "executeCommandLine,executeCommandLine,getSystemEnvVars",
            pclProps.getProperty( "org.codehaus.plexus.util.cli.CommandLineUtils.methods" ) );
        assertNull( pclProps.getProperty( "org.codehaus.plexus.util.RealmDelegatingClassLoader" ) );
    }

}
