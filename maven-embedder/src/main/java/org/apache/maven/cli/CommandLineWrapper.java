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

import java.util.List;

public class CommandLineWrapper
{
    private CommandLine commandLine;

    CommandLineWrapper( CommandLine commandLine )
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
