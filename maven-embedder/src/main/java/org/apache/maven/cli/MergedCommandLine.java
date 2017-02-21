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
import org.apache.commons.cli.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CommandLine} instance that represents a merged command line combining CLI arguments with those from the
 * {@code .mvn/maven.config} while reflecting the handling of {@link CLIManager#SET_SYSTEM_PROPERTY} versus all the
 * other command line options (last wins vs first wins respectively).
 */
class MergedCommandLine
    extends CommandLine
{
    MergedCommandLine( CommandLine commandLine, CommandLine configFile )
    {
        // such a pity that Commons CLI does not offer either a builder or a formatter and we need to extend
        // to perform the merge. A formatter would mean we could unparse and reparse (not ideal but would work).
        // A builder would be ideal for this kind of merge like processing.
        super();
        // the args are easy, cli first then config file
        for ( String arg : commandLine.getArgs() )
        {
            addArg( arg );
        }
        for ( String arg : configFile.getArgs() )
        {
            addArg( arg );
        }
        // now add all options, except for -D with cli first then config file
        List<Option> setPropertyOptions = new ArrayList<>();
        for ( Option opt : commandLine.getOptions() )
        {
            if ( String.valueOf( CLIManager.SET_SYSTEM_PROPERTY ).equals( opt.getOpt() ) )
            {
                setPropertyOptions.add( opt );
            }
            else
            {
                addOption( opt );
            }
        }
        for ( Option opt : configFile.getOptions() )
        {
            addOption( opt );
        }
        // finally add the CLI system properties
        for ( Option opt : setPropertyOptions )
        {
            addOption( opt );
        }
    }

}
