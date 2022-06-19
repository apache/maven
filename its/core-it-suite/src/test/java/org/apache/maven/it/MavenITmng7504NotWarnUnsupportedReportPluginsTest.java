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
package org.apache.maven.it;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test for
 * <a href="https://issues.apache.org/jira/browse/MNG-7504">MNG-7504</a>
 *
 * Warning about unsupported reportPlugins should not be printed for m-site-p.
 *
 * @author Slawomir Jaranowski
 */
public class MavenITmng7504NotWarnUnsupportedReportPluginsTest extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7504-warn-unsupported-report-plugins";

    public MavenITmng7504NotWarnUnsupportedReportPluginsTest()
    {
        super( "[3.9.0]" );
    }

    public void testWarnNotPresent() throws IOException, VerificationException
    {
        File rootDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );

        Verifier verifier = newVerifier( rootDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "site" );
        verifier.resetStreams();

        List<String> logLines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );

        for ( String line : logLines )
        {
            assertFalse( line.contains( "[WARNING] Parameter 'reportPlugins' is unknown for plugin 'maven-site-plugin:0.1-stub-SNAPSHOT:site (default-site)'" ) );
        }
    }
}
