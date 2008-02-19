package org.apache.maven.embedder.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

/** @author Jason van Zyl */
public class EmbedderOfflineTest
    extends AbstractEmbedderExecutionTestCase
{
    private File tempRepo = new File( System.getProperty( "java.io.tmpdir" ), "test-deploy-repository" );

    public void tearDown()
        throws Exception
    {
        if ( tempRepo.exists() && tempRepo.isDirectory() )
        {
            try
            {
                FileUtils.deleteDirectory( tempRepo );
            }
            catch ( IOException e )
            {
                // ignore, we've done our best.
            }
        }

        super.tearDown();
    }

    protected String getId()
    {
        return "offline-from-embedder";
    }

    public void testShouldFailToDeployWhenOfflineFromSettings()
        throws IOException
    {
        File testDirectory = new File( getBasedir(), "src/test/embedder-test-project" );

        File targetDirectory = new File( getBasedir(), "target/offline-from-settings-deploy" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        Settings settings = new Settings();
        settings.setOffline( true );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setSettings( settings )
            .setShowErrors( true )
            .setLoggingLevel( Logger.LEVEL_DEBUG )
            .setBaseDirectory( targetDirectory )
            .setGoals( Collections.singletonList( "deploy" ) );

        MavenExecutionResult result = maven.execute( request );

        assertTrue( "Deployment should have failed.", result.hasExceptions() );

        List exceptions = result.getExceptions();
        assertEquals( 1, exceptions.size() );

        assertTrue( exceptions.get( 0 ) instanceof LifecycleExecutionException );

        LifecycleExecutionException top = (LifecycleExecutionException) exceptions.get( 0 );

        top.printStackTrace();

        assertNotNull( top.getCause().getCause() );
        assertTrue( "Deployment should fail due to offline status.", top.getCause().getCause().getMessage().indexOf( "System is offline" ) > -1 );

    }
}
