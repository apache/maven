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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.building.Source;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;

@Component( role = ConfigurationProcessor.class, hint = SettingsXmlConfigurationProcessor.HINT )
public class SettingsXmlConfigurationProcessor
    implements ConfigurationProcessor
{

    public static final String HINT = "settings";

    public static final String USER_HOME = System.getProperty( "user.home" );

    public static final File USER_MAVEN_CONFIGURATION_HOME = new File( USER_HOME, ".m2" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( USER_MAVEN_CONFIGURATION_HOME, "settings.xml" );

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
        new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    @Requirement
    private Logger logger;

    @Requirement
    private SettingsBuilder settingsBuilder;

    @Requirement
    private SettingsDecrypter settingsDecrypter;

    @Requirement
    private ProfileSelector profileSelector;

    @Override
    public void process( CliRequest cliRequest )
        throws Exception
    {
        CommandLine commandLine = cliRequest.getCommandLine();
        String workingDirectory = cliRequest.getWorkingDirectory();
        MavenExecutionRequest request = cliRequest.getRequest();

        File userSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS ) );
            userSettingsFile = resolveFile( userSettingsFile, workingDirectory );

            if ( !userSettingsFile.isFile() )
            {
                throw new FileNotFoundException( String.format( "The specified user settings file does not exist: %s",
                                                                userSettingsFile ) );

            }
        }
        else
        {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        File globalSettingsFile;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) )
        {
            globalSettingsFile = new File( commandLine.getOptionValue( CLIManager.ALTERNATE_GLOBAL_SETTINGS ) );
            globalSettingsFile = resolveFile( globalSettingsFile, workingDirectory );

            if ( !globalSettingsFile.isFile() )
            {
                throw new FileNotFoundException( String.format( "The specified global settings file does not exist: %s",
                                                                globalSettingsFile ) );

            }
        }
        else
        {
            globalSettingsFile = DEFAULT_GLOBAL_SETTINGS_FILE;
        }

        request.setGlobalSettingsFile( globalSettingsFile );
        request.setUserSettingsFile( userSettingsFile );

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile( globalSettingsFile );
        settingsRequest.setUserSettingsFile( userSettingsFile );
        settingsRequest.setSystemProperties( cliRequest.getSystemProperties() );
        settingsRequest.setUserProperties( cliRequest.getUserProperties() );

        if ( request.getEventSpyDispatcher() != null )
        {
            request.getEventSpyDispatcher().onEvent( settingsRequest );
        }

        logger.debug( String.format( "Reading global settings from %s",
                                     getLocation( settingsRequest.getGlobalSettingsSource(),
                                                  settingsRequest.getGlobalSettingsFile() ) ) );

        logger.debug( String.format( "Reading user settings from %s",
                                     getLocation( settingsRequest.getUserSettingsSource(),
                                                  settingsRequest.getUserSettingsFile() ) ) );

        SettingsBuildingResult settingsResult = settingsBuilder.build( settingsRequest );

        if ( request.getEventSpyDispatcher() != null )
        {
            request.getEventSpyDispatcher().onEvent( settingsResult );
        }

        populateFromSettings( request, settingsResult.getEffectiveSettings() );

        if ( !settingsResult.getProblems().isEmpty() && logger.isWarnEnabled() )
        {
            logger.warn( "" );
            logger.warn( "Some problems were encountered while building the effective settings" );

            for ( SettingsProblem problem : settingsResult.getProblems() )
            {
                logger.warn( problem.getMessage() + " @ " + problem.getLocation() );
            }
            logger.warn( "" );
        }

        // profile activation
        final DefaultProfileActivationContext profileActivationContext = new DefaultProfileActivationContext();
        profileActivationContext.setActiveProfileIds( request.getActiveProfiles() );
        profileActivationContext.setInactiveProfileIds( request.getInactiveProfiles() );
        profileActivationContext.setSystemProperties( request.getSystemProperties() );
        profileActivationContext.setUserProperties( request.getUserProperties() );
        profileActivationContext.setProjectDirectory( ( request.getPom() != null )
                                                          ? request.getPom().getParentFile()
                                                          : null );

        final List<ModelProblem> modelProblems = new ArrayList<>();
        final List<Profile> activeProfiles =
            this.profileSelector.getActiveProfiles( request.getProfiles(), profileActivationContext,
                                                    new ModelProblemCollector()
                                                    {

                                                        @Override
                                                        public void add( final ModelProblemCollectorRequest req )
                                                        {
                                                            modelProblems.add( new DefaultModelProblem(
                                                                    req.getMessage(), req.getSeverity(),
                                                                    req.getVersion(), "settings.xml", -1, -1,
                                                                    null, req.getException() ) );

                                                        }

                                                    } );

        if ( !modelProblems.isEmpty() )
        {
            logger.warn( "" );
            logger.warn( "Some problems were encountered while processing profiles" );

            for ( final ModelProblem problem : modelProblems )
            {
                logger.warn( problem.getMessage(), problem.getException() );
            }

            logger.warn( "" );
        }

        if ( !activeProfiles.isEmpty() )
        {
            for ( final Profile profile : activeProfiles )
            {
                final List<Repository> remoteRepositories = profile.getRepositories();

                for ( final Repository remoteRepository : remoteRepositories )
                {
                    try
                    {
                        request.addRemoteRepository(
                            MavenRepositorySystem.buildArtifactRepository( remoteRepository ) );

                    }
                    catch ( final InvalidRepositoryException e )
                    {
                        logger.warn( String.format( "Failure adding repository '%s' from profile '%s'.",
                                                    remoteRepository.getId(), profile.getId() ), e );

                    }
                }

                final List<Repository> pluginRepositories = profile.getPluginRepositories();

                for ( final Repository pluginRepository : pluginRepositories )
                {
                    try
                    {
                        request.addPluginArtifactRepository(
                            MavenRepositorySystem.buildArtifactRepository( pluginRepository ) );

                    }
                    catch ( InvalidRepositoryException e )
                    {
                        logger.warn( String.format( "Failure adding plugin repository '%s' from profile '%s'.",
                                                    pluginRepository.getId(), profile.getId() ), e );

                    }
                }
            }
        }
    }

    private MavenExecutionRequest populateFromSettings( MavenExecutionRequest request, Settings settings )
        throws MavenExecutionRequestPopulationException
    {
        if ( settings == null )
        {
            return request;
        }

        request.setOffline( settings.isOffline() );

        request.setInteractiveMode( settings.isInteractiveMode() );

        request.setPluginGroups( settings.getPluginGroups() );

        request.setLocalRepositoryPath( settings.getLocalRepository() );

        for ( Server server : settings.getServers() )
        {
            server = server.clone();

            request.addServer( server );
        }

        //  <proxies>
        //    <proxy>
        //      <active>true</active>
        //      <protocol>http</protocol>
        //      <host>proxy.somewhere.com</host>
        //      <port>8080</port>
        //      <username>proxyuser</username>
        //      <password>somepassword</password>
        //      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
        //    </proxy>
        //  </proxies>

        for ( Proxy proxy : settings.getProxies() )
        {
            if ( !proxy.isActive() )
            {
                continue;
            }

            proxy = proxy.clone();

            request.addProxy( proxy );
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for ( Mirror mirror : settings.getMirrors() )
        {
            mirror = mirror.clone();

            request.addMirror( mirror );
        }

        request.addActiveProfiles( settings.getActiveProfiles() );

        for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
        {
            request.addProfile( SettingsUtils.convertFromSettingsProfile( rawProfile ) );
        }

        return request;
    }

    private Object getLocation( Source source, File defaultLocation )
    {
        if ( source != null )
        {
            return source.getLocation();
        }
        return defaultLocation;
    }

    static File resolveFile( File file, String workingDirectory )
    {
        if ( file == null )
        {
            return null;
        }
        else if ( file.isAbsolute() )
        {
            return file;
        }
        else if ( file.getPath().startsWith( File.separator ) )
        {
            // drive-relative Windows path
            return file.getAbsoluteFile();
        }
        else
        {
            return new File( workingDirectory, file.getPath() ).getAbsoluteFile();
        }
    }

}
