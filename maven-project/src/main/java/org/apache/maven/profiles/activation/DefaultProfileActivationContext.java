package org.apache.maven.profiles.activation;

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

public class DefaultProfileActivationContext
    implements ProfileActivationContext
{

    private boolean isCustomActivatorFailureSuppressed;

    private final Properties executionProperties;

    List explicitlyActive;

    List explicitlyInactive;

    private List activeByDefault;

    public DefaultProfileActivationContext( Properties executionProperties, boolean isCustomActivatorFailureSuppressed )
    {
        this.executionProperties = executionProperties;
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

    public List getExplicitlyActiveProfileIds()
    {
        if ( explicitlyActive == null )
        {
            return Collections.EMPTY_LIST;
        }

        return explicitlyActive;
    }

    public void setExplicitlyActiveProfileIds( List active )
    {
        explicitlyActive = active;
    }

    public List getExplicitlyInactiveProfileIds()
    {
        if ( explicitlyInactive == null )
        {
            return Collections.EMPTY_LIST;
        }

        return explicitlyInactive;
    }

    public void setExplicitlyInactiveProfileIds( List inactive )
    {
        explicitlyInactive = inactive;
    }

    public void setActive( String profileId )
    {
        if ( explicitlyActive == null )
        {
            explicitlyActive = new ArrayList();
        }

        explicitlyActive.add( profileId );
    }

    public void setInactive( String profileId )
    {
        if ( explicitlyInactive == null )
        {
            explicitlyInactive = new ArrayList();
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

    public List getActiveByDefaultProfileIds()
    {
        if ( activeByDefault == null )
        {
            return Collections.EMPTY_LIST;
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
            activeByDefault = new ArrayList();
        }

        activeByDefault.add( profileId );
    }

    public void setActiveByDefaultProfileIds( List activeByDefault )
    {
        this.activeByDefault = activeByDefault;
    }

}
