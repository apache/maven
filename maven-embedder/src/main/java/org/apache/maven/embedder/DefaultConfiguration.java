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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Default implementation of Configuration intefrace.
 *
 * @author mkleint
 */
public class DefaultConfiguration
    implements Configuration
{
    private List<String> inactives;

    private List<String> actives;

    private File userSettings;

    private File globalSettings;

    private ContainerCustomizer customizer;

    private Properties systemProperties;

    private List<URL> extensions = new ArrayList<URL>();

    private MavenEmbedderLogger logger;

    private ClassWorld classWorld;

    private PlexusContainer parentContainer;

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

    public Configuration addActiveProfiles( List<String> profiles )
    {
        getActiveProfiles().addAll( profiles );

        return this;
    }

    public Configuration addInactiveProfiles( List<String> profiles )
    {
        getInactiveProfiles().addAll( profiles );

        return this;
    }

    public List<String> getActiveProfiles()
    {
        if ( actives == null )
        {
            actives = new ArrayList<String>();
        }
        return actives;
    }

    public List<String> getInactiveProfiles()
    {
        if ( inactives == null )
        {
            inactives = new ArrayList<String>();
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

    public List<URL> getExtensions()
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

}
