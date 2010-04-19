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

import java.io.File;
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

    private File pomFile;

    private ModelSource modelSource;

    private int validationLevel = VALIDATION_LEVEL_STRICT;

    private boolean processPlugins;

    private boolean twoPhaseBuilding;

    private boolean locationTracking;

    private List<Profile> profiles;

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties systemProperties;

    private Properties userProperties;

    private Date buildStartTime;

    private ModelResolver modelResolver;

    private ModelBuildingListener modelBuildingListener;

    private ModelCache modelCache;

    /**
     * Creates an empty request.
     */
    public DefaultModelBuildingRequest()
    {
    }

    /**
     * Creates a shallow copy of the specified request.
     * 
     * @param request The request to copy, must not be {@code null}.
     */
    public DefaultModelBuildingRequest( ModelBuildingRequest request )
    {
        setPomFile( request.getPomFile() );
        setModelSource( request.getModelSource() );
        setValidationLevel( request.getValidationLevel() );
        setProcessPlugins( request.isProcessPlugins() );
        setTwoPhaseBuilding( request.isTwoPhaseBuilding() );
        setProfiles( request.getProfiles() );
        setActiveProfileIds( request.getActiveProfileIds() );
        setInactiveProfileIds( request.getInactiveProfileIds() );
        setSystemProperties( request.getSystemProperties() );
        setUserProperties( request.getUserProperties() );
        setBuildStartTime( request.getBuildStartTime() );
        setModelResolver( request.getModelResolver() );
        setModelBuildingListener( request.getModelBuildingListener() );
        setModelCache( request.getModelCache() );
    }

    public File getPomFile()
    {
        return pomFile;
    }

    public DefaultModelBuildingRequest setPomFile( File pomFile )
    {
        this.pomFile = ( pomFile != null ) ? pomFile.getAbsoluteFile() : null;

        return this;
    }

    public ModelSource getModelSource()
    {
        return modelSource;
    }

    public DefaultModelBuildingRequest setModelSource( ModelSource modelSource )
    {
        this.modelSource = modelSource;

        return this;
    }

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

    public boolean isTwoPhaseBuilding()
    {
        return twoPhaseBuilding;
    }

    public DefaultModelBuildingRequest setTwoPhaseBuilding( boolean twoPhaseBuilding )
    {
        this.twoPhaseBuilding = twoPhaseBuilding;

        return this;
    }

    public boolean isLocationTracking()
    {
        return locationTracking;
    }

    public DefaultModelBuildingRequest setLocationTracking( boolean locationTracking )
    {
        this.locationTracking = locationTracking;

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

    public Properties getSystemProperties()
    {
        if ( systemProperties == null )
        {
            systemProperties = new Properties();
        }

        return systemProperties;
    }

    public DefaultModelBuildingRequest setSystemProperties( Properties systemProperties )
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

    public DefaultModelBuildingRequest setUserProperties( Properties userProperties )
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

    public ModelBuildingListener getModelBuildingListener()
    {
        return modelBuildingListener;
    }

    public ModelBuildingRequest setModelBuildingListener( ModelBuildingListener modelBuildingListener )
    {
        this.modelBuildingListener = modelBuildingListener;

        return this;
    }

    public ModelCache getModelCache()
    {
        return this.modelCache;
    }

    public DefaultModelBuildingRequest setModelCache( ModelCache modelCache )
    {
        this.modelCache = modelCache;

        return this;
    }

}
