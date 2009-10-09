package org.apache.maven.cli;

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
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;

public class CLIRequestUtilsTest
    extends TestCase
{

    private CommandLine parse( String... args )
        throws ParseException
    {
        return new CLIManager().parse( args );
    }

    public void test_buildRequest_ParseCommandLineProperty()
        throws ParseException
    {
        String key = "key";
        String value = "value";

        String[] args = {
            "-D" + key + "=" + value
        };

        CommandLine commandLine = parse( args );

        assertTrue( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) );

        System.out.println( commandLine.getOptionValue( CLIManager.SET_SYSTEM_PROPERTY ) );
        System.out.println( commandLine.getArgList() );

        assertEquals( 1, commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY ).length );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request = CLIRequestUtils.populateRequest( request, commandLine, null, false, false, false );

        Properties userProperties = request.getUserProperties();

        assertEquals( value, userProperties.getProperty( key ) );

        List<String> goals = request.getGoals();
        assertTrue( ( goals == null ) || goals.isEmpty() );
    }

    public void testGetExecutionProperties()
        throws Exception
    {
        System.setProperty( "test.property.1", "1.0" );
        System.setProperty( "test.property.2", "2.0" );
        Properties execProperties = new Properties();
        Properties userProperties = new Properties();

        CLIRequestUtils.populateProperties( parse( 
            "-Dtest.property.2=2.1",
            "-Dtest.property.3=3.0"
         ), execProperties, userProperties );

        // assume that everybody has a PATH env var
        String envPath = execProperties.getProperty( "env.PATH" );
        String envPath2 = userProperties.getProperty( "env.PATH" );
        if ( envPath == null )
        {
            envPath = execProperties.getProperty( "env.Path" );
            envPath2 = userProperties.getProperty( "env.Path" );
        }

        assertNotNull( envPath );
        assertNull( envPath2 );

        assertEquals( "1.0", execProperties.getProperty( "test.property.1" ) );
        assertNull( userProperties.getProperty( "test.property.1" ) );

        assertEquals( "3.0", execProperties.getProperty( "test.property.3" ) );
        assertEquals( "3.0", userProperties.getProperty( "test.property.3" ) );
    }

    public void testMavenRepoLocal()
        throws Exception
    {
        String path = new File( "" ).getAbsolutePath();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        CLIRequestUtils.populateRequest( request, parse( "-Dmaven.repo.local=" + path ), null, false, false, false );

        assertEquals( path, request.getLocalRepositoryPath().getAbsolutePath() );
    }

}
