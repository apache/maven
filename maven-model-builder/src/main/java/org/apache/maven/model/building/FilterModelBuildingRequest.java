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

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.WorkspaceModelResolver;

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

    FilterModelBuildingRequest( ModelBuildingRequest request )
    {
        this.request = request;
    }

    @Override
    public File getPomFile()
    {
        return request.getPomFile();
    }

    @Override
    public FilterModelBuildingRequest setPomFile( File pomFile )
    {
        request.setPomFile( pomFile );

        return this;
    }

    @Override
    public ModelSource getModelSource()
    {
        return request.getModelSource();
    }

    @Override
    public FilterModelBuildingRequest setModelSource( ModelSource modelSource )
    {
        request.setModelSource( modelSource );

        return this;
    }

    @Override
    public int getValidationLevel()
    {
        return request.getValidationLevel();
    }

    @Override
    public FilterModelBuildingRequest setValidationLevel( int validationLevel )
    {
        request.setValidationLevel( validationLevel );

        return this;
    }

    @Override
    public boolean isProcessPlugins()
    {
        return request.isProcessPlugins();
    }

    @Override
    public FilterModelBuildingRequest setProcessPlugins( boolean processPlugins )
    {
        request.setProcessPlugins( processPlugins );

        return this;
    }

    @Override
    public boolean isTwoPhaseBuilding()
    {
        return request.isTwoPhaseBuilding();
    }

    @Override
    public FilterModelBuildingRequest setTwoPhaseBuilding( boolean twoPhaseBuilding )
    {
        request.setTwoPhaseBuilding( twoPhaseBuilding );

        return this;
    }

    @Override
    public boolean isLocationTracking()
    {
        return request.isLocationTracking();
    }

    @Override
    public FilterModelBuildingRequest setLocationTracking( boolean locationTracking )
    {
        request.setLocationTracking( locationTracking );

        return this;
    }

    @Override
    public List<Profile> getProfiles()
    {
        return request.getProfiles();
    }

    @Override
    public FilterModelBuildingRequest setProfiles( List<Profile> profiles )
    {
        request.setProfiles( profiles );

        return this;
    }

    @Override
    public List<String> getActiveProfileIds()
    {
        return request.getActiveProfileIds();
    }

    @Override
    public FilterModelBuildingRequest setActiveProfileIds( List<String> activeProfileIds )
    {
        request.setActiveProfileIds( activeProfileIds );

        return this;
    }

    @Override
    public List<String> getInactiveProfileIds()
    {
        return request.getInactiveProfileIds();
    }

    @Override
    public FilterModelBuildingRequest setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        request.setInactiveProfileIds( inactiveProfileIds );

        return this;
    }

    @Override
    public Properties getSystemProperties()
    {
        return request.getSystemProperties();
    }

    @Override
    public FilterModelBuildingRequest setSystemProperties( Properties systemProperties )
    {
        request.setSystemProperties( systemProperties );

        return this;
    }

    @Override
    public Properties getUserProperties()
    {
        return request.getUserProperties();
    }

    @Override
    public FilterModelBuildingRequest setUserProperties( Properties userProperties )
    {
        request.setUserProperties( userProperties );

        return this;
    }

    @Override
    public Date getBuildStartTime()
    {
        return request.getBuildStartTime();
    }

    @Override
    public ModelBuildingRequest setBuildStartTime( Date buildStartTime )
    {
        request.setBuildStartTime( buildStartTime );

        return this;
    }

    @Override
    public ModelResolver getModelResolver()
    {
        return request.getModelResolver();
    }

    @Override
    public FilterModelBuildingRequest setModelResolver( ModelResolver modelResolver )
    {
        request.setModelResolver( modelResolver );

        return this;
    }

    @Override
    public ModelBuildingListener getModelBuildingListener()
    {
        return request.getModelBuildingListener();
    }

    @Override
    public ModelBuildingRequest setModelBuildingListener( ModelBuildingListener modelBuildingListener )
    {
        request.setModelBuildingListener( modelBuildingListener );

        return this;
    }

    @Override
    public ModelCache getModelCache()
    {
        return request.getModelCache();
    }

    @Override
    public FilterModelBuildingRequest setModelCache( ModelCache modelCache )
    {
        request.setModelCache( modelCache );

        return this;
    }

    @Override
    public Model getFileModel()
    {
        return request.getFileModel();
    }

    @Override
    public ModelBuildingRequest setFileModel( Model fileModel )
    {
        request.setFileModel( fileModel );
        return this;
    }

    @Override
    public Model getRawModel()
    {
        return request.getRawModel();
    }

    @Override
    public ModelBuildingRequest setRawModel( Model rawModel )
    {
        request.setRawModel( rawModel );
        return this;
    }

    @Override
    public WorkspaceModelResolver getWorkspaceModelResolver()
    {
        return request.getWorkspaceModelResolver();
    }

    @Override
    public ModelBuildingRequest setWorkspaceModelResolver( WorkspaceModelResolver workspaceResolver )
    {
        request.setWorkspaceModelResolver( workspaceResolver );
        return this;
    }

    @Override
    public TransformerContextBuilder getTransformerContextBuilder()
    {
        return request.getTransformerContextBuilder();
    }

    @Override
    public ModelBuildingRequest setTransformerContextBuilder( TransformerContextBuilder contextBuilder )
    {
        request.setTransformerContextBuilder( contextBuilder );
        return this;
    }
}