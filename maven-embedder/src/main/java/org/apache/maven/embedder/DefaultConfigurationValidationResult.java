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
    private Exception userSettingsException;

    private Exception globalSettingsException;

    private Settings userSettings, globalSettings;

    public boolean isValid()
    {
        return ( getUserSettingsException() == null ) && ( getGlobalSettingsException() == null );
    }

    public Exception getUserSettingsException()
    {
        return userSettingsException;
    }

    public void setUserSettingsException( Exception e )
    {
        this.userSettingsException = e;
    }

    public Exception getGlobalSettingsException()
    {
        return globalSettingsException;
    }

    public void setGlobalSettingsException( Exception e )
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
        return isSettingsFilePresent( getGlobalSettings(), getGlobalSettingsException() );
    }

    public boolean isUserSettingsFileParses()
    {
        return getUserSettings() != null;
    }

    public boolean isUserSettingsFilePresent()
    {
        return isSettingsFilePresent( getUserSettings(), getUserSettingsException() );
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

    private boolean isSettingsFilePresent( Settings settings, Throwable e )
    {
        return ( settings != null ) || ( ( e != null ) && !( e instanceof FileNotFoundException ) );
    }
}
