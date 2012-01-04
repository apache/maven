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
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.mortbay.jetty.Server;

import java.io.File;
import java.io.FileReader;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-5224">MNG-5175</a>.
 * test correct injection of settings with profiles in ${settings} in mojo
 *
 * @version $Id$
 */
public class MavenITmng5224InjectedSettings
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    public MavenITmng5224InjectedSettings()
    {
        // olamy probably doesn't work with 3.x before 3.0.4
        super( "[2.0.3,3.0-alpha-1),[3.0.4,)" );
    }


    /**
     *
     */
    public void testmng5224_ReadSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5224" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.getCliOptions().add( "-U" );
        verifier.getCliOptions().add( "--settings" );
        verifier.getCliOptions().add( "settings.xml" );
        //verifier.
        verifier.executeGoal( "validate" );

        File settingsFile = new File( verifier.getBasedir(), "target/settings-dump.xml" );

        FileReader fr = new FileReader( settingsFile );

        Xpp3Dom dom = Xpp3DomBuilder.build( fr );

        Xpp3Dom profilesNode = dom.getChild( "profiles" );

        Xpp3Dom[] profileNodes = profilesNode.getChildren( "profile" );

        // 3 from the user settings + 1 for the global settings used for its
        assertEquals( 4, profileNodes.length );

    }


}
