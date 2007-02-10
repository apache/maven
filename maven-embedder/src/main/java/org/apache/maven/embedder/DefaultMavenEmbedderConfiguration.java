package org.apache.maven.embedder;
/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.settings.Settings;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Default implementation of MavenEmbedderConfiguration intefrace.
 *
 * @author mkleint
 */
public class DefaultMavenEmbedderConfiguration
    implements MavenEmbedderConfiguration
{
    private List inactives;

    private List actives;

    private Settings settings;

    private File userSettings;

    private File globalSettings;

    private ContainerCustomizer customizer;

    private Properties systemProperties;

    /** List&lt;URL>. */
    private List extensions = new ArrayList();

    private MavenEmbedderLogger logger;

    private ClassWorld classWorld;

    /** Creates a new instance of DefaultMavenEmbedderConfiguration */
    public DefaultMavenEmbedderConfiguration()
    {
    }

    public MavenEmbedderConfiguration addActiveProfile( String profile )
    {
        getActiveProfiles().add( profile );
        return this;
    }

    public MavenEmbedderConfiguration addInactiveProfile( String profile )
    {
        getInactiveProfiles().add( profile );
        return this;
    }

    public MavenEmbedderConfiguration addActiveProfiles( List profiles )
    {
        getActiveProfiles().addAll( profiles );
        return this;
    }

    public MavenEmbedderConfiguration addInactiveProfiles( List profiles )
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

    public MavenEmbedderConfiguration setUserSettingsFile( File user )
    {
        userSettings = user;
        return this;
    }

    public MavenEmbedderConfiguration setGlobalSettingsFile( File global )
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

    public MavenEmbedderConfiguration setConfigurationCustomizer( ContainerCustomizer customizer )
    {
        this.customizer = customizer;
        return this;
    }

    public ContainerCustomizer getContainerCustomizer()
    {
        return customizer;
    }

    public MavenEmbedderConfiguration setSystemProperties( Properties properties )
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

    public MavenEmbedderConfiguration setMavenEmbedderLogger( MavenEmbedderLogger logger )
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

    public MavenEmbedderConfiguration setClassWorld( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
        return this;
    }

    public MavenEmbedderConfiguration setClassLoader( ClassLoader loader )
    {
        this.classWorld = new ClassWorld( "plexus.core", loader );
        return this;
    }
}
