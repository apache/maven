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
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Profile;
import org.apache.maven.model.resolution.ModelResolver;

/**
 * A model building request that delegates all methods invocations to another request, meant for easy transformations by
 * subclassing.
 * 
 * @author Benjamin Bentmann
 */
class FilterModelBuildingRequest
    implements ModelBuildingRequest
{

    protected ModelBuildingRequest request;

    public FilterModelBuildingRequest( ModelBuildingRequest request )
    {
        this.request = request;
    }

    public File getPomFile()
    {
        return request.getPomFile();
    }

    public FilterModelBuildingRequest setPomFile( File pomFile )
    {
        request.setPomFile( pomFile );

        return this;
    }

    public ModelSource getModelSource()
    {
        return request.getModelSource();
    }

    public FilterModelBuildingRequest setModelSource( ModelSource modelSource )
    {
        request.setModelSource( modelSource );

        return this;
    }

    public int getValidationLevel()
    {
        return request.getValidationLevel();
    }

    public FilterModelBuildingRequest setValidationLevel( int validationLevel )
    {
        request.setValidationLevel( validationLevel );

        return this;
    }

    public boolean isProcessPlugins()
    {
        return request.isProcessPlugins();
    }

    public FilterModelBuildingRequest setProcessPlugins( boolean processPlugins )
    {
        request.setProcessPlugins( processPlugins );

        return this;
    }

    public boolean isTwoPhaseBuilding()
    {
        return request.isTwoPhaseBuilding();
    }

    public FilterModelBuildingRequest setTwoPhaseBuilding( boolean twoPhaseBuilding )
    {
        request.setTwoPhaseBuilding( twoPhaseBuilding );

        return this;
    }

    public boolean isLocationTracking()
    {
        return request.isLocationTracking();
    }

    public FilterModelBuildingRequest setLocationTracking( boolean locationTracking )
    {
        request.setLocationTracking( locationTracking );

        return this;
    }

    public List<Profile> getProfiles()
    {
        return request.getProfiles();
    }

    public FilterModelBuildingRequest setProfiles( List<Profile> profiles )
    {
        request.setProfiles( profiles );

        return this;
    }

    public List<String> getActiveProfileIds()
    {
        return request.getActiveProfileIds();
    }

    public FilterModelBuildingRequest setActiveProfileIds( List<String> activeProfileIds )
    {
        request.setActiveProfileIds( activeProfileIds );

        return this;
    }

    public List<String> getInactiveProfileIds()
    {
        return request.getInactiveProfileIds();
    }

    public FilterModelBuildingRequest setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        request.setInactiveProfileIds( inactiveProfileIds );

        return this;
    }

    public Properties getSystemProperties()
    {
        return request.getSystemProperties();
    }

    public FilterModelBuildingRequest setSystemProperties( Properties systemProperties )
    {
        request.setSystemProperties( systemProperties );

        return this;
    }

    public Properties getUserProperties()
    {
        return request.getUserProperties();
    }

    public FilterModelBuildingRequest setUserProperties( Properties userProperties )
    {
        request.setUserProperties( userProperties );

        return this;
    }

    public Date getBuildStartTime()
    {
        return request.getBuildStartTime();
    }

    public ModelBuildingRequest setBuildStartTime( Date buildStartTime )
    {
        request.setBuildStartTime( buildStartTime );

        return this;
    }

    public ModelResolver getModelResolver()
    {
        return request.getModelResolver();
    }

    public FilterModelBuildingRequest setModelResolver( ModelResolver modelResolver )
    {
        request.setModelResolver( modelResolver );

        return this;
    }

    public ModelBuildingListener getModelBuildingListener()
    {
        return request.getModelBuildingListener();
    }

    public ModelBuildingRequest setModelBuildingListener( ModelBuildingListener modelBuildingListener )
    {
        request.setModelBuildingListener( modelBuildingListener );

        return this;
    }

    public ModelCache getModelCache()
    {
        return request.getModelCache();
    }

    public FilterModelBuildingRequest setModelCache( ModelCache modelCache )
    {
        request.setModelCache( modelCache );

        return this;
    }

}
