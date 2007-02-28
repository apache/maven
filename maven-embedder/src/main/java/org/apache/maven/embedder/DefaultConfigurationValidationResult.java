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

/** @author Jason van Zyl */
public class DefaultConfigurationValidationResult
    implements ConfigurationValidationResult
{
    private boolean userSettingsFilePresent = true;

    private boolean userSettingsFileParses = true;

    private boolean globalSettingsFilePresent = true;

    private boolean globalSettingsFileParses = true;

    public boolean isValid()
    {
        return userSettingsFilePresent && userSettingsFileParses && globalSettingsFilePresent &&
            globalSettingsFileParses;
    }

    public boolean isUserSettingsFilePresent()
    {
        return userSettingsFilePresent;
    }

    public void setUserSettingsFilePresent( boolean userSettingsFilePresent )
    {
        this.userSettingsFilePresent = userSettingsFilePresent;
    }

    public boolean isUserSettingsFileParses()
    {
        return userSettingsFileParses;
    }

    public void setUserSettingsFileParses( boolean userSettingsFileParses )
    {
        this.userSettingsFileParses = userSettingsFileParses;
    }

    public boolean isGlobalSettingsFilePresent()
    {
        return globalSettingsFilePresent;
    }

    public void setGlobalSettingsFilePresent( boolean globalSettingsFilePresent )
    {
        this.globalSettingsFilePresent = globalSettingsFilePresent;
    }

    public boolean isGlobalSettingsFileParses()
    {
        return globalSettingsFileParses;
    }

    public void setGlobalSettingsFileParses( boolean globalSettingsFileParses )
    {
        this.globalSettingsFileParses = globalSettingsFileParses;
    }

    public void display()
    {
        System.out.println( "userSettingsFilePresent = " + userSettingsFilePresent );
        System.out.println( "globalSettingsFileParses = " + globalSettingsFileParses );
        System.out.println( "globalSettingsFilePresent = " + globalSettingsFilePresent );
        System.out.println( "globalSettingsFileParses = " + globalSettingsFileParses );
    }
}
