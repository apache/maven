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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4162">MNG-4162</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4162ReportingMigrationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4162ReportingMigrationTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that the legacy reporting section is automatically converted into ordinary plugin configuration of the
     * Maven Site Plugin to ease migration.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4162" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String p = "project.build.plugins.0.executions.0.configuration.children.";

        Properties props = verifier.loadProperties( "target/site.properties" );
        assertTrue( props.getProperty( p + "outputDirectory.0.value" ).endsWith( "other-site" ) );
        assertEquals( "maven-surefire-report-plugin",
            props.getProperty( p + "reportPlugins.0.children.reportPlugin.0.children.artifactId.0.value" ) );
        assertEquals( "maven-project-info-reports-plugin",
            props.getProperty( p + "reportPlugins.0.children.reportPlugin.1.children.artifactId.0.value" ) );
        assertEquals( "report",
            props.getProperty( p + "reportPlugins.0.children.reportPlugin.0.children.reportSets.0.children.reportSet.0.children.reports.0.children.report.0.value" ) );
        assertEquals( "report-only",
            props.getProperty( p + "reportPlugins.0.children.reportPlugin.0.children.reportSets.0.children.reportSet.1.children.reports.0.children.report.0.value" ) );
        assertEquals( "true",
            props.getProperty( p + "reportPlugins.0.children.reportPlugin.0.children.reportSets.0.children.reportSet.0.children.configuration.0.children.skipTests.0.value" ) );
        assertEquals( "false",
            props.getProperty( p + "reportPlugins.0.children.reportPlugin.0.children.reportSets.0.children.reportSet.1.children.configuration.0.children.skipTests.0.value" ) );
    }

}
