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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3506">MNG-3506</a>.
 * 
 * @author John Casey
 * @version $Id$
 */
public class MavenITmng4254SelectableWagonProvidersTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4254SelectableWagonProvidersTest()
    {
        super( "(2.2.0,)" );
    }

    public void testCliUsage()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4254" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-Dmaven.wagon.provider.http=coreit" );

        verifier.setLogFileName( "log-cli.txt" );
        verifier.executeGoal( "deploy" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertTrue( "target/wagon.properties should exist.", new File( testDir, "target/wagon.properties" ).exists() );
    }

    public void testSettingsUsage()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4254" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "--settings" );
        cliOptions.add( "settings.xml" );

        verifier.setLogFileName( "log-settings.txt" );
        verifier.executeGoal( "deploy" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertTrue( "target/wagon.properties should exist.", new File( testDir, "target/wagon.properties" ).exists() );
    }
}
