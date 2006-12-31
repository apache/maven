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

/**
 * Default implementation of MavenEmbedRequest intefrace.
 *
 * @author mkleint
 */
public class DefaultMavenEmbedRequest
    implements MavenEmbedRequest
{
    private List inactives;

    private List actives;

    private Settings settings;

    private File userSettings;

    private File globalSettings;

    private ContainerCustomizer customizer;

    private Properties systemProperties;

    /**
     * Creates a new instance of DefaultMavenEmbedRequest
     */
    public DefaultMavenEmbedRequest()
    {
    }

    public MavenEmbedRequest addActiveProfile( String profile )
    {
        getActiveProfiles().add( profile );
        return this;
    }

    public MavenEmbedRequest addInactiveProfile( String profile )
    {
        getInactiveProfiles().add( profile );
        return this;
    }

    public MavenEmbedRequest addActiveProfiles( List profiles )
    {
        getActiveProfiles().addAll( profiles );
        return this;
    }

    public MavenEmbedRequest addInactiveProfiles( List profiles )
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

    public MavenEmbedRequest setUserSettingsFile( File user )
    {
        userSettings = user;
        return this;
    }

    public MavenEmbedRequest setGlobalSettingsFile( File global )
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

    public MavenEmbedRequest setConfigurationCustomizer( ContainerCustomizer customizer )
    {
        this.customizer = customizer;
        return this;
    }

    public ContainerCustomizer getContainerCustomizer()
    {
        return customizer;
    }

    public MavenEmbedRequest setSystemProperties(Properties properties) 
    {
        systemProperties = properties;
        return this;
    }

    public Properties getSystemProperties() 
    {
        return systemProperties != null ? systemProperties : System.getProperties();
    }

}
