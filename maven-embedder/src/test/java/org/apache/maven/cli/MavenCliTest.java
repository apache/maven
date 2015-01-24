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

import junit.framework.TestCase;

import org.apache.commons.cli.ParseException;

public class MavenCliTest
    extends TestCase
{
    private MavenCli cli;

    private String origBasedir;

    protected void setUp()
    {
        cli = new MavenCli();
        origBasedir = System.getProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( origBasedir != null )
        {
            System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY, origBasedir );
        }
        else
        {
            System.getProperties().remove( MavenCli.MULTIMODULE_PROJECT_DIRECTORY );
        }
        super.tearDown();
    }

    public void testCalculateDegreeOfConcurrencyWithCoreMultiplier()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        // -T2.2C
        assertEquals( (int) ( cores * 2.2 ), cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "C2.2" ) );
        // -TC2.2
        assertEquals( (int) ( cores * 2.2 ), cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "2.2C" ) );

        try
        {
            cli.calculateDegreeOfConcurrencyWithCoreMultiplier( "CXXX" );
            fail( "Should have failed with a NumberFormatException" );
        }
        catch ( NumberFormatException e )
        {
            // carry on
        }
    }

    public void testMavenConfig()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY, new File( "src/test/projects/config" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[0], null );

        // read .mvn/maven.config
        cli.initialize( request );
        cli.cli( request );
        assertEquals( "multithreaded", request.commandLine.getOptionValue( "builder" ) );
        assertEquals( "8", request.commandLine.getOptionValue( "threads" ) );

        // override from command line
        request = new CliRequest( new String[] { "--builder", "foobar" }, null );
        cli.cli( request );
        assertEquals( "foobar", request.commandLine.getOptionValue( "builder" ) );
    }

    public void testMavenConfigInvalid()
        throws Exception
    {
        System.setProperty( MavenCli.MULTIMODULE_PROJECT_DIRECTORY, new File( "src/test/projects/config-illegal" ).getCanonicalPath() );
        CliRequest request = new CliRequest( new String[0], null );

        cli.initialize( request );
        try
        {
            cli.cli( request );
            fail();
        }
        catch ( ParseException expected )
        {

        }
    }
}
