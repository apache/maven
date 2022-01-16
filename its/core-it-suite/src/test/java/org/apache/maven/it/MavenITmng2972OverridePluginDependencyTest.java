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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2972">MNG-2972</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2972OverridePluginDependencyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2972OverridePluginDependencyTest()
    {
        super( "(2.0.8,)" );
    }

    /**
     * Verify that a project-level plugin dependency replaces the original dependency from the plugin POM.
     *
     * @throws Exception in case of failure
     */
    public void testitLifecycleInvocation()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2972/test1" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifact( "org.apache.maven.its.plugins.class-loader", "dep-b", "0.2-mng-2972", "jar" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps = verifier.loadProperties( "target/pcl.properties" );
        verify( pclProps );
    }

    /**
     * Verify that a project-level plugin dependency replaces the original dependency from the plugin POM.
     * Apart from testing direct CLI invocation this time, this test also employs a slightly different version for the
     * overriding dependency. The original bug is caused by usage of a HashSet but whenever the random order of its
     * elements happens to match the correct ordering, the test cannot detect the bad implementation. The obvious way
     * to increase the test coverage is re-running the test with different dependency versions, each time producing
     * another hash code for the dependency artifact and thereby changing its position in the HashSet's element order.
     * The two versions 0.2-mng-2972 and 9.9-MNG-2972 we use here have at least once proven (on Sun JDK 1.6.0_07) to
     * successfully break the correctness of the random ordering.
     *
     * @throws Exception in case of failure
     */
    public void testitCommandLineInvocation()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2972/test2" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifact( "org.apache.maven.its.plugins.class-loader", "dep-b", "9.9-MNG-2972", "jar" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-class-loader:2.1-SNAPSHOT:load" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps = verifier.loadProperties( "target/pcl.properties" );
        verify( pclProps );
    }

    private void verify( Properties pclProps )
        throws Exception
    {
        assertNotNull( pclProps.getProperty( "org.apache.maven.its.mng2972.MNG2972" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassA" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassB" ) );
        assertEquals( "1", pclProps.getProperty( "org/apache/maven/its/mng2972/mng-2972.properties.count" ) );
        assertEquals( "0", pclProps.getProperty( "org/apache/maven/plugin/coreit/a.properties.count" ) );
        assertEquals( "0", pclProps.getProperty( "org/apache/maven/plugin/coreit/b.properties.count" ) );
    }

}
