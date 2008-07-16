package org.apache.maven.embedder.validation;

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

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class MavenEmbedderCrappySettingsConfigurationTest
    extends PlexusTestCase
{
    public void testEmbedderWillStillStartupWhenTheSettingsConfigurationIsCrap()
        throws Exception
    {
        // START SNIPPET: simple-embedder-example

        File projectDirectory = getTestFile( "src/examples/simple-project" );

        File user = new File( projectDirectory, "invalid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user )
            .setClassLoader( Thread.currentThread().getContextClassLoader() );

        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );

        assertFalse( validationResult.isValid() );

        MavenEmbedder embedder = new MavenEmbedder( configuration );

        assertNotNull( embedder.getLocalRepository().getBasedir() );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBaseDirectory( projectDirectory )
            .setGoals( Arrays.asList( new String[]{"validate"} ) );

        MavenExecutionResult result = embedder.execute( request );
        
        for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
        {
            Exception e = (Exception) i.next();
            e.printStackTrace();
        }
        
        assertFalse( result.hasExceptions() );

        assertNotNull( result.getProject() );

        MavenProject project = result.getProject();

        String environment = project.getProperties().getProperty( "environment" );

        assertEquals( "development", environment );

        // END SNIPPET: simple-embedder-example
    }
}
