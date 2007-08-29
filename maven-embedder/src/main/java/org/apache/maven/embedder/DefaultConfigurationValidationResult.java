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

import java.io.FileNotFoundException;

import org.apache.maven.settings.Settings;

/**
 * @author Jason van Zyl
 */
public class DefaultConfigurationValidationResult
    implements ConfigurationValidationResult
{
    private Throwable userSettingsException;

    private Throwable globalSettingsException;

    private Settings userSettings, globalSettings;

    public boolean isValid()
    {
        return ( getUserSettings() != null ) && ( getGlobalSettings() != null );
    }

    public Throwable getUserSettingsException()
    {
        return userSettingsException;
    }

    public void setUserSettingsException( Throwable e )
    {
        this.userSettingsException = e;
    }

    public Throwable getGlobalSettingsException()
    {
        return globalSettingsException;
    }

    public void setGlobalSettingsException( Throwable e )
    {
        this.globalSettingsException = e;
    }

    public Settings getUserSettings()
    {
        return userSettings;
    }

    public void setUserSettings( Settings settings )
    {
        this.userSettings = settings;
    }

    public Settings getGlobalSettings()
    {
        return globalSettings;
    }

    public void setGlobalSettings( Settings globalSettings )
    {
        this.globalSettings = globalSettings;
    }

    public boolean isGlobalSettingsFileParses()
    {
        return getGlobalSettings() != null;
    }

    public boolean isGlobalSettingsFilePresent()
    {
        return getGlobalSettingsException() instanceof FileNotFoundException;
    }

    public boolean isUserSettingsFileParses()
    {
        return getUserSettings() != null;
    }

    public boolean isUserSettingsFilePresent()
    {
        return getUserSettingsException() instanceof FileNotFoundException;
    }

    public void setGlobalSettingsFileParses( boolean globalSettingsFileParses )
    {
        // ignored
    }

    public void setGlobalSettingsFilePresent( boolean globalSettingsFilePresent )
    {
        // ignored
    }

    public void setUserSettingsFileParses( boolean userSettingsFileParses )
    {
        // ignored
    }

    public void setUserSettingsFilePresent( boolean userSettingsFilePresent )
    {
        // ignored
    }

    public void display()
    {
        // ignored
    }
}
