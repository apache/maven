package org.apache.maven.integrationtests;

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
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng2972OverridePluginDependency
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2972OverridePluginDependency()
    {
        super( "(2.0.8,)" );
    }

    public void testitMNG2972()
        throws Exception
    {

        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2972-overridingPluginDependency" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created by this test has been removed from the local repository.
         * Failing to do this could cause unstable test results. Fortunately, the verifier makes it easy to do this.
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng2972", "user", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2972", "mojo", "0.0.1-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2972", "dep", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2972", "dep", "2.0", "jar" );

        verifier = new Verifier( new File( testDir.getAbsolutePath(), "dep1" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        verifier = new Verifier( new File( testDir.getAbsolutePath(), "dep2" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        verifier = new Verifier( new File( testDir.getAbsolutePath(), "mojo" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        verifier = new Verifier( new File( testDir.getAbsolutePath(), "user" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        List lines =
            verifier.loadFile( new File( testDir.getAbsolutePath(), "user" ).getAbsolutePath(), "log.txt", false );
        int foundVersionOne = 0;
        int foundVersionTwo = 0;
        for ( Iterator i = lines.iterator(); i.hasNext(); )
        {

            String line = (String) i.next();
            if ( line.indexOf( "MNG-2972-VERSION-1" ) != -1 )
                foundVersionOne++;
            if ( line.indexOf( "MNG-2972-VERSION-2" ) != -1 )
                foundVersionTwo++;
        }

        verifier.resetStreams();

        Assert.assertEquals( "Should not be using plugin dependency version 1", 0, foundVersionOne );
        Assert.assertEquals( "Should be using plugin version 2 once.", 1, foundVersionTwo );

        /**
         * Now try to execute the plugin directly
         */

        verifier = new Verifier( new File( testDir.getAbsolutePath(), "user" ).getAbsolutePath() );
        verifier.executeGoal( "org.apache.maven.its.mng2972:mojo:0.0.1-SNAPSHOT:test" );
        verifier.verifyErrorFreeLog();

        lines = verifier.loadFile( new File( testDir.getAbsolutePath(), "user" ).getAbsolutePath(), "log.txt", false );
        foundVersionOne = 0;
        foundVersionTwo = 0;
        for ( Iterator i = lines.iterator(); i.hasNext(); )
        {

            String line = (String) i.next();
            if ( line.indexOf( "MNG-2972-VERSION-1" ) != -1 )
                foundVersionOne++;
            if ( line.indexOf( "MNG-2972-VERSION-2" ) != -1 )
                foundVersionTwo++;
        }

        verifier.resetStreams();

        Assert.assertEquals( "Should not be using plugin dependency version 1", 0, foundVersionOne );
        Assert.assertEquals( "Should be using plugin version 2 once.", 1, foundVersionTwo );
    }
}
