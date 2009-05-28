package org.apache.maven.profiles;

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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Deprecated
public class ProfileActivationContext
    implements org.apache.maven.model.profile.ProfileActivationContext
{
    private boolean isCustomActivatorFailureSuppressed;

    private final Properties executionProperties;

    private List<String> explicitlyActive;

    private List<String> explicitlyInactive;

    private List<String> activeByDefault;

    public ProfileActivationContext( Properties executionProperties, boolean isCustomActivatorFailureSuppressed )
    {
        this.executionProperties = (executionProperties != null) ? executionProperties : new Properties();
        this.isCustomActivatorFailureSuppressed = isCustomActivatorFailureSuppressed;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public boolean isCustomActivatorFailureSuppressed()
    {
        return isCustomActivatorFailureSuppressed;
    }

    public void setCustomActivatorFailureSuppressed( boolean suppressed )
    {
        isCustomActivatorFailureSuppressed = suppressed;
    }

    public List<String> getExplicitlyActiveProfileIds()
    {
        if ( explicitlyActive == null )
        {
            return Collections.emptyList();
        }

        return explicitlyActive;
    }

    public void setExplicitlyActiveProfileIds( List<String> active )
    {
        explicitlyActive = active;
    }

    public List<String> getExplicitlyInactiveProfileIds()
    {
        if ( explicitlyInactive == null )
        {
            return Collections.emptyList();
        }

        return explicitlyInactive;
    }

    public void setExplicitlyInactiveProfileIds( List<String> inactive )
    {
        explicitlyInactive = inactive;
    }

    public void setActive( String profileId )
    {
        if ( explicitlyActive == null )
        {
            explicitlyActive = new ArrayList<String>();
        }

        explicitlyActive.add( profileId );
    }

    public void setInactive( String profileId )
    {
        if ( explicitlyInactive == null )
        {
            explicitlyInactive = new ArrayList<String>();
        }

        explicitlyInactive.add( profileId );
    }

    public boolean isExplicitlyActive( String profileId )
    {
        return ( explicitlyActive != null ) && explicitlyActive.contains( profileId );
    }

    public boolean isExplicitlyInactive( String profileId )
    {
        return ( explicitlyInactive != null ) && explicitlyInactive.contains( profileId );
    }

    public List<String> getActiveByDefaultProfileIds()
    {
        if ( activeByDefault == null )
        {
            return Collections.emptyList();
        }

        return activeByDefault;
    }

    public boolean isActiveByDefault( String profileId )
    {
        return ( activeByDefault != null ) && activeByDefault.contains( profileId );
    }

    public void setActiveByDefault( String profileId )
    {
        if ( activeByDefault == null )
        {
            activeByDefault = new ArrayList<String>();
        }

        activeByDefault.add( profileId );
    }

    public void setActiveByDefaultProfileIds( List<String> activeByDefault )
    {
        this.activeByDefault = activeByDefault;
    }

    public List<String> getActiveProfileIds()
    {
        return getExplicitlyActiveProfileIds();
    }

    public List<String> getInactiveProfileIds()
    {
        return getExplicitlyInactiveProfileIds();
    }

}
