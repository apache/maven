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

import org.apache.maven.model.resolution.ModelResolver;

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

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties executionProperties;

    private ModelResolver modelResolver;
    
    public DefaultModelBuildingRequest()
    {
        profiles = new ArrayList<Profile>();
        activeProfileIds = new ArrayList<String>();
        inactiveProfileIds = new ArrayList<String>();
        executionProperties = new Properties();
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
        return activeProfileIds;
    }

    public DefaultModelBuildingRequest setActiveProfileIds( List<String> activeProfileIds )
    {
        this.activeProfileIds.clear();
        if ( activeProfileIds != null )
        {
            this.activeProfileIds.addAll( activeProfileIds );
        }

        return this;
    }

    public List<String> getInactiveProfileIds()
    {
        return inactiveProfileIds;
    }

    public DefaultModelBuildingRequest setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        this.inactiveProfileIds.clear();
        if ( inactiveProfileIds != null )
        {
            this.inactiveProfileIds.addAll( inactiveProfileIds );
        }

        return this;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public DefaultModelBuildingRequest setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties.clear();
        if ( executionProperties != null )
        {
            this.executionProperties.putAll( executionProperties );
        }

        return this;
    }

    public ModelResolver getModelResolver()
    {
        return this.modelResolver;
    }

    public DefaultModelBuildingRequest setModelResolver( ModelResolver modelResolver )
    {
        this.modelResolver = modelResolver;

        return this;
    }

}
