package org.apache.maven.settings.building;

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
import java.util.Properties;

/**
 * Collects settings that control building of effective settings.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuildingRequest
    implements SettingsBuildingRequest
{

    private File globalSettingsFile;

    private File userSettingsFile;

    private SettingsSource globalSettingsSource;

    private SettingsSource userSettingsSource;

    private Properties systemProperties;

    private Properties userProperties;

    public File getGlobalSettingsFile()
    {
        return globalSettingsFile;
    }

    public DefaultSettingsBuildingRequest setGlobalSettingsFile( File globalSettingsFile )
    {
        this.globalSettingsFile = globalSettingsFile;

        return this;
    }

    public SettingsSource getGlobalSettingsSource()
    {
        return globalSettingsSource;
    }

    public DefaultSettingsBuildingRequest setGlobalSettingsSource( SettingsSource globalSettingsSource )
    {
        this.globalSettingsSource = globalSettingsSource;

        return this;
    }

    public File getUserSettingsFile()
    {
        return userSettingsFile;
    }

    public DefaultSettingsBuildingRequest setUserSettingsFile( File userSettingsFile )
    {
        this.userSettingsFile = userSettingsFile;

        return this;
    }

    public SettingsSource getUserSettingsSource()
    {
        return userSettingsSource;
    }

    public DefaultSettingsBuildingRequest setUserSettingsSource( SettingsSource userSettingsSource )
    {
        this.userSettingsSource = userSettingsSource;

        return this;
    }

    public Properties getSystemProperties()
    {
        if ( systemProperties == null )
        {
            systemProperties = new Properties();
        }

        return systemProperties;
    }

    public DefaultSettingsBuildingRequest setSystemProperties( Properties systemProperties )
    {
        if ( systemProperties != null )
        {
            this.systemProperties = new Properties();
            this.systemProperties.putAll( systemProperties );
        }
        else
        {
            this.systemProperties = null;
        }

        return this;
    }

    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        return userProperties;
    }

    public DefaultSettingsBuildingRequest setUserProperties( Properties userProperties )
    {
        if ( userProperties != null )
        {
            this.userProperties = new Properties();
            this.userProperties.putAll( userProperties );
        }
        else
        {
            this.userProperties = null;
        }

        return this;
    }

}
