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
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2972">MNG-2972</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng2972OverridePluginDependency
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2972OverridePluginDependency()
    {
        super( "(2.0.8,)" );
    }

    /**
     * Verify that a project-level plugin dependency replaces the original dependency from the plugin POM.
     */
    public void testitLifecycleInvocation()
        throws Exception
    {
        run( false );
    }

    /**
     * Verify that a project-level plugin dependency replaces the original dependency from the plugin POM.
     */
    public void testitCommandLineInvocation()
        throws Exception
    {
        run( true );
    }

    private void run( boolean cli )
        throws Exception
    {
        String propFile = cli ? "target/cli.properties" : "target/lifecycle.properties";

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2972" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifact( "org.apache.maven.its.plugins.class-loader", "dep-b", "0.2-mng-2972", "jar" );
        new File( testDir, propFile ).delete();
        Properties sysProps = new Properties();
        sysProps.setProperty( "clsldr.pluginClassLoaderOutput", propFile );
        verifier.setSystemProperties( sysProps );
        verifier.executeGoal( cli ? "org.apache.maven.its.plugins:maven-it-plugin-class-loader:2.1-SNAPSHOT:load" : "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        FileUtils.rename( new File( testDir, "log.txt"), new File( testDir, cli ? "log2.txt" : "log1.txt" ) );

        Properties pclProps = verifier.loadProperties( propFile );
        assertNotNull( pclProps.getProperty( "org.apache.maven.its.mng2972.MNG2972" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassA" ) );
        assertNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassB" ) );
        assertEquals( "1", pclProps.getProperty( "org/apache/maven/its/mng2972/mng-2972.properties.count" ) );
        assertEquals( "0", pclProps.getProperty( "org/apache/maven/plugin/coreit/a.properties.count" ) );
        assertEquals( "0", pclProps.getProperty( "org/apache/maven/plugin/coreit/b.properties.count" ) );
    }

}
