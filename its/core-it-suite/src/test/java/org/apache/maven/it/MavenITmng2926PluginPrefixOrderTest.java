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

    public MavenITmng2926PluginPrefixOrderTest()
    {
        super( "(2.0.6,)" );
    }

    /**
     * Verify that when resolving plugin prefixes the group org.apache.maven.plugins is searched before
     * org.codehaus.mojo and that custom groups from the settings are searched before these standard ones.
     */
    public void testitMNG2926()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2926" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2926" );
        verifier.deleteArtifacts( "org.apache.maven.plugins", "mng-2926", "0.1" );
        verifier.deleteArtifacts( "org.apache.maven.plugins", "mng-2926", "0.1" );
        new File( verifier.localRepo, "org/apache/maven/plugins/maven-metadata-maven-core-it.xml" ).delete();
        new File( verifier.localRepo, "org/apache/maven/plugins/resolver-status.properties" ).delete();
        verifier.deleteArtifacts( "org.codehaus.mojo", "mng-2926", "0.1" );
        verifier.deleteArtifacts( "org.codehaus.mojo", "mng-2926", "0.1" );
        new File( verifier.localRepo, "org/codehaus/mojo/maven-metadata-maven-core-it.xml" ).delete();
        new File( verifier.localRepo, "org/codehaus/mojo/resolver-status.properties" ).delete();
        verifier.resetStreams();

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-default.txt" );
        verifier.filterFile( "settings-default-template.xml", "settings-default.xml", "UTF-8", 
            verifier.newDefaultFilterProperties() );
        verifier.getCliOptions().add( "--settings" );
        verifier.getCliOptions().add( "settings-default.xml" );
        verifier.executeGoal( "mng-2926:apache" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-custom.txt" );
        verifier.filterFile( "settings-custom-template.xml", "settings-custom.xml", "UTF-8", 
            verifier.newDefaultFilterProperties() );
        verifier.getCliOptions().add( "--settings" );
        verifier.getCliOptions().add( "settings-custom.xml" );
        verifier.executeGoal( "mng-2926:custom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
