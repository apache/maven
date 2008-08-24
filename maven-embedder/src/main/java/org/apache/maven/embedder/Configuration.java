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

import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.monitor.event.MavenWorkspaceMonitor;
import org.apache.maven.realm.MavenRealmManager;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Configuration of embedder, used when starting up.
 *
 * @author mkleint
 * @author Jason van Zyl
 */
public interface Configuration
{
    // ----------------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------------

    /** Set location of the userSettingsFile settings file to use for the embedder. */
    Configuration setUserSettingsFile( File userSettingsFile );

    File getUserSettingsFile();

    /** Set location of the globalSettingsFiles settings file to use for the embedder. */
    Configuration setGlobalSettingsFile( File globalSettingsFiles );

    File getGlobalSettingsFile();

    // ----------------------------------------------------------------------------
    // Local Repository
    // ----------------------------------------------------------------------------

    /**
     * This overrides anything set in a settings file.
     */
    Configuration setLocalRepository( File localRepository );

    File getLocalRepository();

    // ----------------------------------------------------------------------------
    // Logger
    // ----------------------------------------------------------------------------

    Configuration setMavenEmbedderLogger( MavenEmbedderLogger logger );

    MavenEmbedderLogger getMavenEmbedderLogger();

    // ----------------------------------------------------------------------------
    // ClassWorld/ClassLoader
    // ----------------------------------------------------------------------------

    ClassWorld getClassWorld();

    Configuration setClassWorld( ClassWorld classWorld );

    Configuration setClassLoader( ClassLoader loader );

    PlexusContainer getParentContainer();

    Configuration setParentContainer( PlexusContainer parentContainer );

    // ----------------------------------------------------------------------------
    // Profiles
    // ----------------------------------------------------------------------------

    /** Add profile to activate. */
    Configuration addActiveProfile( String profile );

    /** Add profile to inactivate. */
    Configuration addInactiveProfile( String profile );

    /** Add a list of String instances with names of profiles to activate. */
    Configuration addActiveProfiles( List profiles );

    /** Add a list of String instances with names of profiles to inactivate. */
    Configuration addInactiveProfiles( List profiles );

    /** Set a customizer callback implemetation that will be given a chance to modify the plexus container on startup. */
    Configuration setConfigurationCustomizer( ContainerCustomizer customizer );

    /** set the system properties to be used during the lifecycle of the embedder. Excluding the time when executing the project, then the properties from MavenExecutionRequestare used. */
    Configuration setSystemProperties( Properties properties );

    List getActiveProfiles();

    List getInactiveProfiles();

    // ----------------------------------------------------------------------------
    // Container Customizer
    // ----------------------------------------------------------------------------

    ContainerCustomizer getContainerCustomizer();

    // ----------------------------------------------------------------------------
    // System Properties
    // ----------------------------------------------------------------------------

    Properties getSystemProperties();

    // ----------------------------------------------------------------------------
    // Extensions
    // ----------------------------------------------------------------------------

    void addExtension( URL url );

    List getExtensions();

    Configuration setRealmManager( MavenRealmManager realmManager );

    MavenRealmManager getRealmManager();

    // ----------------------------------------------------------------------------
    // Error Reporter
    // ----------------------------------------------------------------------------

    Configuration setErrorReporter( CoreErrorReporter errorReporter );

    CoreErrorReporter getErrorReporter();

    // ----------------------------------------------------------------------------
    // Event Monitors
    // ----------------------------------------------------------------------------

    Configuration addEventMonitor( EventMonitor eventMonitor );

    Configuration setEventMonitors( List eventMonitors );

    List getEventMonitors();

    Configuration setWorkspaceMonitor( MavenWorkspaceMonitor workspaceMonitor );

    MavenWorkspaceMonitor getWorkspaceMonitor();
}
