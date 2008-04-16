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

import org.apache.maven.embedder.AbstractEmbedderTestCase;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public abstract class AbstractEmbedderExecutionTestCase
    extends AbstractEmbedderTestCase
{
    protected File runWithProject( String goal )
        throws Exception
    {
        return runWithProject( Collections.singletonList( goal ), null );
    }

    protected File runWithProject( String goal,
                                   Properties properties )
        throws Exception
    {
        return runWithProject( Collections.singletonList( goal ), properties );
    }

    protected File runWithProject( String[] goals )
        throws Exception
    {
        return runWithProject( Arrays.asList( goals ), null );
    }

    protected File runWithProject( String[] goals,
                                   Properties properties )
        throws Exception
    {
        return runWithProject( Arrays.asList( goals ), properties );
    }

    protected File runWithProject( List goals )
        throws Exception
    {
        return runWithProject( goals, null );
    }

    protected File runWithProject( List goals,
                                   Properties properties )
        throws Exception
    {
        /*
        if ( request.getBaseDirectory() == null || !new File( request.getBaseDirectory() ).exists() )
        {
            throw new IllegalStateException( "You must specify a valid base directory in your execution request for this test." );
        }
        */

        File testDirectory = new File( getBasedir(), "src/test/embedder-test-project" );

        File targetDirectory = new File( getBasedir(), "target/" + getId() );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setShowErrors( true )
            //.setLoggingLevel( MavenExecutionRequest.LOGGING_LEVEL_DEBUG )
            .setBaseDirectory( targetDirectory )
            .setGoals( goals );

        System.out.println( "properties = " + properties );

        if ( properties != null )
        {
            request.setProperties( properties );
        }

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

        return targetDirectory;
    }

    protected abstract String getId();

    protected void assertNoExceptions( MavenExecutionResult result )
    {
        if ( !result.hasExceptions() )
        {
            return;
        }

        for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
        {
            Exception exception = (Exception) i.next();

            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }

    protected void assertFileExists( File file )
    {
        if ( !file.exists() )
        {
            fail( "The specified file '" + file + "' does not exist." );
        }
    }
}
