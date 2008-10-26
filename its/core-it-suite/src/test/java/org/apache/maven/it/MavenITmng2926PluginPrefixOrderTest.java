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

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2926">MNG-2926</a>
 * 
 * @author Brian Fox
 * @version $Id$
 */
public class MavenITmng2926PluginPrefixOrderTest
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG2926()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2926" );

        Verifier verifier;
        //use my custom settings upon invocation.
        ArrayList cli = new ArrayList();

        // Install the parent POM, extension and the plugin
        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        // 2008-09-29 Oleg: fixed the test. If current settings.xml contains codehause group, 
        // default order will be changed. Artificially make currently set groups disappear
        
        // now run the test. Since we have apache and codehaus, i should get the apache one first
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2926/test-project" );
        cli.add("-s '" +testDir.getAbsolutePath()+"/settings-apache.xml'");
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setCliOptions( cli );
        verifier.executeGoal( "it0119:apache" );
        verifier.verifyErrorFreeLog();

        cli.clear();
//      now run the test. Since we have apache and codehaus and a prefix in my settings, i should get the custom one first
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2926/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        
        cli.add("-s '" +testDir.getAbsolutePath()+"/settings.xml'");
        verifier.setCliOptions( cli );
        verifier.executeGoal( "it0119:custom" );
        verifier.verifyErrorFreeLog();
    }
}
