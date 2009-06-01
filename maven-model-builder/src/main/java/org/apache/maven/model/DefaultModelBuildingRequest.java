package org.apache.maven.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;

/**
 * Collects settings that control building of effective models.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultModelBuildingRequest
    implements ModelBuildingRequest
{

    private boolean lenientValidation;

    private boolean processPlugins;

    private List<Profile> profiles;

    private ProfileActivationContext profileActivationContext;

    public DefaultModelBuildingRequest()
    {
        profiles = new ArrayList<Profile>();
        profileActivationContext = new DefaultProfileActivationContext();
    }

    public boolean istLenientValidation()
    {
        return lenientValidation;
    }

    public DefaultModelBuildingRequest setLenientValidation( boolean lenientValidation )
    {
        this.lenientValidation = lenientValidation;

        return this;
    }

    public boolean isProcessPlugins()
    {
        return processPlugins;
    }

    public DefaultModelBuildingRequest setProcessPlugins( boolean processPlugins )
    {
        this.processPlugins = processPlugins;

        return this;
    }

    public List<Profile> getProfiles()
    {
        return profiles;
    }

    public DefaultModelBuildingRequest setProfiles( List<Profile> profiles )
    {
        this.profiles.clear();
        if ( profiles != null )
        {
            this.profiles.addAll( profiles );
        }

        return this;
    }

    public List<String> getActiveProfileIds()
    {
        return profileActivationContext.getActiveProfileIds();
    }

    public ModelBuildingRequest setActiveProfileIds( List<String> activeProfileIds )
    {
        profileActivationContext.setActiveProfileIds( activeProfileIds );
        return this;
    }

    public List<String> getInactiveProfileIds()
    {
        return profileActivationContext.getInactiveProfileIds();
    }

    public ModelBuildingRequest setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        profileActivationContext.setInactiveProfileIds( inactiveProfileIds );
        return this;
    }

    public Properties getExecutionProperties()
    {
        return profileActivationContext.getExecutionProperties();
    }

    public ModelBuildingRequest setExecutionProperties( Properties executionProperties )
    {
        profileActivationContext.setExecutionProperties( executionProperties );
        return this;
    }

    public ProfileActivationContext getProfileActivationContext()
    {
        return profileActivationContext;
    }

    public DefaultModelBuildingRequest setProfileActivationContext( ProfileActivationContext profileActivationContext )
    {
        if ( profileActivationContext == null )
        {
            throw new IllegalArgumentException( "no profile activation context specified" );
        }

        this.profileActivationContext = profileActivationContext;

        return this;
    }

}
