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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4580">MNG-4580</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4580ProjectLevelPluginDepUsedForCliInvocInReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4580ProjectLevelPluginDepUsedForCliInvocInReactorTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that project-level plugin dependencies of sub modules are still considered when a plugin is invoked
     * directly from command line at the reactor root. In other words, the plugin realm used for a mojo execution
     * should match the plugin dependencies as given by the current project.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4580" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub/target" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-class-loader:load" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps;

        pclProps = verifier.loadProperties( "target/pcl.properties" );
        assertNotNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassA" ) );
        assertNotNull( pclProps.getProperty( "org/apache/maven/plugin/coreit/a.properties" ) );

        pclProps = verifier.loadProperties( "sub/target/pcl.properties" );
        assertNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassA" ) );
        assertNull( pclProps.getProperty( "org/apache/maven/plugin/coreit/a.properties" ) );
    }

}
