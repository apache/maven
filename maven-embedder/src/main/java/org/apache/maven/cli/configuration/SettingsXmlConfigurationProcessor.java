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

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.building.Source;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

import static org.apache.maven.bridge.MavenRepositorySystem.buildArtifactRepository;
import static org.apache.maven.settings.SettingsUtils.convertFromSettingsProfile;

@Component( role = ConfigurationProcessor.class, hint = SettingsXmlConfigurationProcessor.HINT )
public class SettingsXmlConfigurationProcessor
    implements ConfigurationProcessor
{
    public static final String HINT = "settings";

    public static final String USER_HOME = System.getProperty( "user.home" );

    public static final File USER_MAVEN_CONFIGURATION_HOME = new File( USER_HOME, ".m2" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( USER_MAVEN_CONFIGURATION_HOME, "settings.xml" );

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File( System.getProperty( "maven.home", System
        .getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    private SettingsBuildingRequestMapper requestMapper = new SettingsBuildingRequestMapper();

    @Requirement
    private Logger logger;

    @Requirement
    private SettingsBuilder settingsBuilder;

    @Override
    public void process( CliRequest cliRequest )
        throws Exception
    {
        SettingsBuildingRequest settingsRequest = requestMapper.mapFromCliRequest( cliRequest );

        MavenExecutionRequest request = cliRequest.getRequest();
        request.setGlobalSettingsFile( settingsRequest.getGlobalSettingsFile() );
        request.setUserSettingsFile( settingsRequest.getUserSettingsFile() );

        if ( request.getEventSpyDispatcher() != null )
        {
            request.getEventSpyDispatcher().onEvent( settingsRequest );
        }

        logger.debug( "Reading global settings from "
            + getLocation( settingsRequest.getGlobalSettingsSource(), settingsRequest.getGlobalSettingsFile() ) );
        logger.debug( "Reading user settings from "
            + getLocation( settingsRequest.getUserSettingsSource(), settingsRequest.getUserSettingsFile() ) );

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
    }

    MavenExecutionRequest populateFromSettings( MavenExecutionRequest request, Settings settings )
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

        addAllMirrors( request, settings.getMirrors() );

        request.setActiveProfiles( settings.getActiveProfiles() );

        for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
        {
            request.addProfile( convertFromSettingsProfile( rawProfile ) );

            if ( settings.getActiveProfiles().contains( rawProfile.getId() ) )
            {
                for ( Repository remoteRepository : rawProfile.getRepositories() )
                {
                    try
                    {
                        request.addRemoteRepository( buildArtifactRepository( remoteRepository ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        // do nothing for now
                    }
                }

                for ( Repository pluginRepository : rawProfile.getPluginRepositories() )
                {
                    try
                    {
                        request.addPluginArtifactRepository( buildArtifactRepository( pluginRepository ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        // do nothing for now
                    }
                }

                addAllMirrors( request, rawProfile.getMirrors() );
            }
        }
        return request;
    }

    private void addAllMirrors( MavenExecutionRequest request, List<Mirror> mirrors )
    {
        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for ( Mirror mirror : mirrors )
        {
            mirror = mirror.clone();

            request.addMirror( mirror );
        }
    }

    private Object getLocation( Source source, File defaultLocation )
    {
        if ( source != null )
        {
            return source.getLocation();
        }
        return defaultLocation;
    }
}
