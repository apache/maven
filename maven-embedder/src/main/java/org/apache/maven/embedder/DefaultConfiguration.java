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
import org.apache.maven.realm.MavenRealmManager;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Default implementation of Configuration intefrace.
 *
 * @author mkleint
 */
public class DefaultConfiguration
    implements Configuration
{
    private List inactives;

    private List actives;

    private File userSettings;

    private File globalSettings;

    private ContainerCustomizer customizer;

    private Properties systemProperties;

    /** List&lt;URL&gt;. */
    private List extensions = new ArrayList();

    private MavenEmbedderLogger logger;

    private ClassWorld classWorld;

    private PlexusContainer parentContainer;

    private File localRepository;

    private MavenRealmManager realmManager;

    private CoreErrorReporter errorReporter;

    /** List&lt;EventMonitor&gt;. */
    private List eventMonitors;

    /** Creates a new instance of DefaultConfiguration */
    public DefaultConfiguration()
    {
    }

    public Configuration addActiveProfile( String profile )
    {
        getActiveProfiles().add( profile );

        return this;
    }

    public Configuration addInactiveProfile( String profile )
    {
        getInactiveProfiles().add( profile );

        return this;
    }

    public Configuration addActiveProfiles( List profiles )
    {
        getActiveProfiles().addAll( profiles );

        return this;
    }

    public Configuration addInactiveProfiles( List profiles )
    {
        getInactiveProfiles().addAll( profiles );

        return this;
    }

    public List getActiveProfiles()
    {
        if ( actives == null )
        {
            actives = new ArrayList();
        }
        return actives;
    }

    public List getInactiveProfiles()
    {
        if ( inactives == null )
        {
            inactives = new ArrayList();
        }
        return inactives;
    }

    public Configuration setUserSettingsFile( File user )
    {
        userSettings = user;
        return this;
    }

    public Configuration setGlobalSettingsFile( File global )
    {
        globalSettings = global;
        return this;
    }

    public File getUserSettingsFile()
    {
        return userSettings;
    }

    public File getGlobalSettingsFile()
    {
        return globalSettings;
    }

    public Configuration setConfigurationCustomizer( ContainerCustomizer customizer )
    {
        this.customizer = customizer;
        return this;
    }

    public ContainerCustomizer getContainerCustomizer()
    {
        return customizer;
    }

    public Configuration setSystemProperties( Properties properties )
    {
        systemProperties = properties;
        return this;
    }

    public Properties getSystemProperties()
    {
        return systemProperties != null ? systemProperties : System.getProperties();
    }

    public void addExtension( URL url )
    {
        extensions.add( url );
    }

    public List getExtensions()
    {
        return extensions;
    }

    public Configuration setMavenEmbedderLogger( MavenEmbedderLogger logger )
    {
        this.logger = logger;
        return this;
    }

    public MavenEmbedderLogger getMavenEmbedderLogger()
    {
        return logger;
    }

    public ClassWorld getClassWorld()
    {
        return classWorld;
    }

    public Configuration setClassWorld( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
        return this;
    }

    public Configuration setClassLoader( ClassLoader loader )
    {
        classWorld = new ClassWorld( "plexus.core", loader );

        return this;
    }

    public PlexusContainer getParentContainer()
    {
        return parentContainer;
    }

    public Configuration setParentContainer( PlexusContainer parentContainer )
    {
        this.parentContainer = parentContainer;
        return this;
    }

    // ----------------------------------------------------------------------------
    // Local Repository
    // ----------------------------------------------------------------------------

    public Configuration setLocalRepository( File localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public File getLocalRepository()
    {
        return localRepository;
    }

    public MavenRealmManager getRealmManager()
    {
        return realmManager;
    }

    public Configuration setRealmManager( MavenRealmManager realmManager )
    {
        this.realmManager = realmManager;
        return this;
    }

    public CoreErrorReporter getErrorReporter()
    {
        return errorReporter;
    }

    public Configuration setErrorReporter( CoreErrorReporter errorReporter )
    {
        this.errorReporter = errorReporter;
        return this;
    }

    public Configuration addEventMonitor( EventMonitor eventMonitor )
    {
        if ( eventMonitors == null )
        {
            eventMonitors = new ArrayList();
        }

        eventMonitors.add( eventMonitor );

        return this;
    }

    public List getEventMonitors()
    {
        return eventMonitors;
    }

    public Configuration setEventMonitors( List eventMonitors )
    {
        this.eventMonitors = eventMonitors;
        return this;
    }
}
