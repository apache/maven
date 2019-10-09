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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.List;

public class CommandLineWrapper
{
    private CommandLine commandLine;

    static CommandLineWrapper parse( Options options, String[] args ) throws ParseException
    {
        // We need to eat any quotes surrounding arguments...
        String[] cleanArgs = CleanArgument.cleanArgs( args );

        CommandLineParser parser = new GnuParser();

        return new CommandLineWrapper( parser.parse( options, cleanArgs ) );
    }

    CommandLineWrapper mergeMavenConfig( CommandLineWrapper mavenConfig )
    {
        CommandLine.Builder commandLineBuilder = new CommandLine.Builder();

        // the args are easy, cli first then config file
        for ( String arg : this.getArgs() )
        {
            commandLineBuilder.addArg( arg );
        }
        for ( String arg : mavenConfig.getArgs() )
        {
            commandLineBuilder.addArg( arg );
        }

        // now add all options, except for -D with cli first then config file
        List<Option> setPropertyOptions = new ArrayList<>();
        for ( Option opt : this.getOptions() )
        {
            if ( String.valueOf( CLIManager.SET_SYSTEM_PROPERTY ).equals( opt.getOpt() ) )
            {
                setPropertyOptions.add( opt );
            }
            else
            {
                commandLineBuilder.addOption( opt );
            }
        }
        for ( Option opt : mavenConfig.getOptions() )
        {
            commandLineBuilder.addOption( opt );
        }
        // finally add the CLI system properties
        for ( Option opt : setPropertyOptions )
        {
            commandLineBuilder.addOption( opt );
        }
        return new CommandLineWrapper( commandLineBuilder.build() );
    }

    private CommandLineWrapper( CommandLine commandLine )
    {
        this.commandLine = commandLine;
    }

    public List<String> getArgList()
    {
        return commandLine.getArgList();
    }

    public String[] getArgs()
    {
        return commandLine.getArgs();
    }

    public Option[] getOptions()
    {
        return commandLine.getOptions();
    }

    public boolean hasOption( char opt )
    {
        return commandLine.hasOption( opt );
    }

    public boolean hasOption( String opt )
    {
        return commandLine.hasOption( opt );
    }

    public String getOptionValue( char opt )
    {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( String opt )
    {
        return commandLine.getOptionValue( opt );
    }

    public String[] getOptionValues( char opt )
    {
        return commandLine.getOptionValues( opt );
    }

    public String[] getOptionValues( String opt )
    {
        return commandLine.getOptionValues( opt );
    }
}
