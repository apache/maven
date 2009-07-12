package org.apache.maven.embedder;

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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

public class MavenEmbedderAligningBasedirTest
    extends TestCase
{
    protected String basedir;

    protected MavenEmbedder mavenEmbedder;


    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }

        Configuration configuration = new SimpleConfiguration();

        mavenEmbedder = new MavenEmbedder( configuration );
    }

    protected void tearDown()
        throws Exception
    {
        mavenEmbedder.stop();
    }

    protected void assertNoExceptions( MavenExecutionResult result )
    {
        List<Exception> exceptions = result.getExceptions();
        if ( ( exceptions == null ) || exceptions.isEmpty() )
        {
            // everything is a-ok.
            return;
        }

        System.err.println( "Encountered " + exceptions.size() + " exception(s)." );
        for ( Exception exception : exceptions )
        {
            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }

    // ----------------------------------------------------------------------
    // Goal/Phase execution tests
    // ----------------------------------------------------------------------

    public void testExecutionUsingABaseDirectory()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project0" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBaseDirectory( targetDirectory )
            .setShowErrors( true )
            .setGoals( Arrays.asList( new String[]{"package"} ) );

        MavenExecutionResult result = mavenEmbedder.execute( request );

        assertNoExceptions( result );

        MavenProject project = result.getProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }
}
