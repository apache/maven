package org.apache.maven.model.building;

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
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Profile;
import org.apache.maven.model.resolution.ModelResolver;

/**
 * Collects settings that control building of effective models.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultModelBuildingRequest
    implements ModelBuildingRequest
{

    private int validationLevel = VALIDATION_LEVEL_STRICT;

    private boolean processPlugins;

    private List<Profile> profiles;

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties executionProperties;

    private Date buildStartTime;

    private ModelResolver modelResolver;

    private List<ModelBuildingListener> modelBuildingListeners;

    public int getValidationLevel()
    {
        return validationLevel;
    }

    public DefaultModelBuildingRequest setValidationLevel( int validationLevel )
    {
        this.validationLevel = validationLevel;

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
        if ( profiles == null )
        {
            profiles = new ArrayList<Profile>();
        }

        return profiles;
    }

    public DefaultModelBuildingRequest setProfiles( List<Profile> profiles )
    {
        if ( profiles != null )
        {
            this.profiles = new ArrayList<Profile>( profiles );
        }
        else
        {
            this.profiles = null;
        }

        return this;
    }

    public List<String> getActiveProfileIds()
    {
        if ( activeProfileIds == null )
        {
            activeProfileIds = new ArrayList<String>();
        }

        return activeProfileIds;
    }

    public DefaultModelBuildingRequest setActiveProfileIds( List<String> activeProfileIds )
    {
        if ( activeProfileIds != null )
        {
            this.activeProfileIds = new ArrayList<String>( activeProfileIds );
        }
        else
        {
            this.activeProfileIds = null;
        }

        return this;
    }

    public List<String> getInactiveProfileIds()
    {
        if ( inactiveProfileIds == null )
        {
            inactiveProfileIds = new ArrayList<String>();
        }

        return inactiveProfileIds;
    }

    public DefaultModelBuildingRequest setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        if ( inactiveProfileIds != null )
        {
            this.inactiveProfileIds = new ArrayList<String>( inactiveProfileIds );
        }
        else
        {
            this.inactiveProfileIds = null;
        }

        return this;
    }

    public Properties getExecutionProperties()
    {
        if ( executionProperties == null )
        {
            executionProperties = new Properties();
        }

        return executionProperties;
    }

    public DefaultModelBuildingRequest setExecutionProperties( Properties executionProperties )
    {
        if ( executionProperties != null )
        {
            this.executionProperties = new Properties();
            this.executionProperties.putAll( executionProperties );
        }
        else
        {
            this.executionProperties = null;
        }

        return this;
    }

    public Date getBuildStartTime()
    {
        return buildStartTime;
    }

    public ModelBuildingRequest setBuildStartTime( Date buildStartTime )
    {
        this.buildStartTime = buildStartTime;

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

    public List<ModelBuildingListener> getModelBuildingListeners()
    {
        if ( modelBuildingListeners == null )
        {
            modelBuildingListeners = new ArrayList<ModelBuildingListener>();
        }

        return modelBuildingListeners;
    }

    public ModelBuildingRequest setModelBuildingListeners( List<? extends ModelBuildingListener> modelBuildingListeners )
    {
        if ( modelBuildingListeners != null )
        {
            this.modelBuildingListeners = new ArrayList<ModelBuildingListener>( modelBuildingListeners );
        }
        else
        {
            this.modelBuildingListeners = null;
        }

        return this;
    }

}
