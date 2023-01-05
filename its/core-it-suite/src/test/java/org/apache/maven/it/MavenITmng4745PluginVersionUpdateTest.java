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
import java.util.Map;
import java.util.Properties;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4745">MNG-4745</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4745PluginVersionUpdateTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4745PluginVersionUpdateTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-2,)" );
    }

    /**
     * Verify that the update policy of a (plugin) repository affects the check for newer plugin versions.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRepoPolicyAlways()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4745" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setForkJvm( true );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4745" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@updates@", "always" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        writeMetadata( testDir, "1.0", "20100729123455" );
        verifier.setLogFileName( "log-1a.txt" );
        verifier.addCliArgument( "org.apache.maven.its.mng4745:maven-it-plugin:touch" );
        verifier.execute();

        writeMetadata( testDir, "1.1", "20100730123456" );
        verifier.setLogFileName( "log-1b.txt" );
        verifier.addCliArgument( "org.apache.maven.its.mng4745:maven-it-plugin:touch" );
        verifier.execute();

        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/plugin.properties" );
        assertEquals( "1.1", props.get( "plugin.version" ) );
    }

    /**
     * Verify that the update policy of a (plugin) repository affects the check for newer plugin versions.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRepoPolicyNever()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4745" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setForkJvm( true );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4745" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@updates@", "never" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        writeMetadata( testDir, "1.0", "20100729123455" );
        verifier.setLogFileName( "log-2a.txt" );
        verifier.addCliArgument( "org.apache.maven.its.mng4745:maven-it-plugin:touch" );
        verifier.execute();

        writeMetadata( testDir, "1.1", "20100730123456" );
        verifier.setLogFileName( "log-2b.txt" );
        verifier.addCliArgument( "org.apache.maven.its.mng4745:maven-it-plugin:touch" );
        verifier.execute();

        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/plugin.properties" );
        assertEquals( "1.0", props.get( "plugin.version" ) );
    }

    /**
     * Verify that the CLI's force update flag affects the check for newer plugin versions.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitForceUpdate()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4745" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setForkJvm( true );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4745" );
        verifier.addCliArgument( "-U" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );
        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@updates@", "never" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        writeMetadata( testDir, "1.0", "20100729123455" );
        verifier.setLogFileName( "log-3a.txt" );
        verifier.addCliArgument( "org.apache.maven.its.mng4745:maven-it-plugin:touch" );
        verifier.execute();

        writeMetadata( testDir, "1.1", "20100730123456" );
        verifier.setLogFileName( "log-3b.txt" );
        verifier.addCliArgument( "org.apache.maven.its.mng4745:maven-it-plugin:touch" );
        verifier.execute();

        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/plugin.properties" );
        assertEquals( "1.1", props.get( "plugin.version" ) );
    }

    private static void writeMetadata( File testdir, String version, String timestamp )
        throws Exception
    {
        StringBuilder content = new StringBuilder( 1024 );
        content.append( "<?xml version=\"1.0\"?>\n" );
        content.append( "<metadata>\n" );
        content.append( "  <groupId>org.apache.maven.its.mng4745</groupId>\n" );
        content.append( "  <artifactId>maven-it-plugin</artifactId>\n" );
        content.append( "  <versioning>\n" );
        content.append( "    <latest>" + version + "</latest>\n" );
        content.append( "    <release>" + version + "</release>\n" );
        content.append( "    <versions>\n" );
        content.append( "      <version>1.0</version>\n" );
        content.append( "    </versions>\n" );
        content.append( "    <lastUpdated>" + timestamp + "</lastUpdated>\n" );
        content.append( "  </versioning>\n" );
        content.append( "</metadata>\n" );

        File metadata = new File( testdir, "repo/org/apache/maven/its/mng4745/maven-it-plugin/maven-metadata.xml" );
        metadata.getParentFile().mkdirs();
        FileUtils.fileWrite( metadata.getAbsolutePath(), "UTF-8", content.toString() );
    }

}
