package org.apache.maven.cli.configuration;

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

import org.apache.commons.cli.CommandLine;
import org.apache.maven.cli.ActiveProfileArgumentParser;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.FileResolver;
import org.apache.maven.settings.building.CommandLineSettings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;

import java.io.File;
import java.io.FileNotFoundException;

import static org.apache.maven.cli.CLIManager.ALTERNATE_GLOBAL_SETTINGS;
import static org.apache.maven.cli.CLIManager.ALTERNATE_USER_SETTINGS;
import static org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor.DEFAULT_GLOBAL_SETTINGS_FILE;
import static org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;

class SettingsBuildingRequestMapper
{

    private FileResolver fileResolver = new FileResolver();

    SettingsBuildingRequest mapFromCliRequest( CliRequest cliRequest ) throws FileNotFoundException
    {
        CommandLine commandLine = cliRequest.getCommandLine();
        String workingDirectory = cliRequest.getWorkingDirectory();

        File userSettingsFile = resolveOptionalUserSettingsFile( commandLine, workingDirectory );
        File globalSettingsFile = resolveOptionalGlobalSettingsFile( commandLine, workingDirectory );

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile( globalSettingsFile );
        settingsRequest.setUserSettingsFile( userSettingsFile );
        settingsRequest.setSystemProperties( cliRequest.getSystemProperties() );
        settingsRequest.setUserProperties( cliRequest.getUserProperties() );

        mapCommandLineSettings( settingsRequest.getCommandLineSettings(), commandLine );

        return settingsRequest;
    }

    private void mapCommandLineSettings( CommandLineSettings commandLineSettings, CommandLine commandLine )
    {
        ActiveProfileArgumentParser activeProfileArguments = new ActiveProfileArgumentParser( commandLine );
        commandLineSettings.addActiveProfiles( activeProfileArguments.getActiveProfiles() );
        commandLineSettings.addInActiveProfiles( activeProfileArguments.getInactiveProfiles() );
    }

    private File resolveOptionalGlobalSettingsFile( CommandLine commandLine, String workingDirectory )
        throws FileNotFoundException
    {
        File globalSettingsFile;

        if ( commandLine.hasOption( ALTERNATE_GLOBAL_SETTINGS ) )
        {
            globalSettingsFile = new File( commandLine.getOptionValue( ALTERNATE_GLOBAL_SETTINGS ) );
            globalSettingsFile = fileResolver.resolveFile( globalSettingsFile, workingDirectory );

            if ( !globalSettingsFile.isFile() )
            {
                throw new FileNotFoundException( "The specified global settings file does not exist: "
                    + globalSettingsFile );
            }
        }
        else
        {
            globalSettingsFile = DEFAULT_GLOBAL_SETTINGS_FILE;
        }
        return globalSettingsFile;
    }

    private File resolveOptionalUserSettingsFile( CommandLine commandLine, String workingDirectory )
        throws FileNotFoundException
    {
        File userSettingsFile;

        if ( commandLine.hasOption( ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsFile = new File( commandLine.getOptionValue( ALTERNATE_USER_SETTINGS ) );
            userSettingsFile = fileResolver.resolveFile( userSettingsFile, workingDirectory );

            if ( !userSettingsFile.isFile() )
            {
                throw new FileNotFoundException( "The specified user settings file does not exist: "
                    + userSettingsFile );
            }
        }
        else
        {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }
        return userSettingsFile;
    }
}
