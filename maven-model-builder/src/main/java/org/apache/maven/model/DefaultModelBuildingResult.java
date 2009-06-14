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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the output of the model builder.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultModelBuildingResult
    implements ModelBuildingResult
{

    private Model model;

    private List<Model> rawModels;

    private Map<Model, List<Profile>> activePomProfiles;

    private List<Profile> activeExternalProfiles;

    public DefaultModelBuildingResult()
    {
        rawModels = new ArrayList<Model>();
        activePomProfiles = new HashMap<Model, List<Profile>>();
        activeExternalProfiles = new ArrayList<Profile>();
    }

    public Model getEffectiveModel()
    {
        return model;
    }

    public DefaultModelBuildingResult setEffectiveModel( Model model )
    {
        this.model = model;

        return this;
    }

    public Model getRawModel()
    {
        return rawModels.get( 0 );
    }

    public List<Model> getRawModels()
    {
        return Collections.unmodifiableList( rawModels );
    }

    public DefaultModelBuildingResult setRawModels( List<Model> rawModels )
    {
        this.rawModels.clear();
        if ( rawModels != null )
        {
            this.rawModels.addAll( rawModels );
        }

        return this;
    }

    public List<Profile> getActivePomProfiles( Model rawModel )
    {
        List<Profile> profiles = this.activePomProfiles.get( rawModel );
        return ( profiles == null ) ? Collections.<Profile> emptyList() : Collections.unmodifiableList( profiles );
    }

    public DefaultModelBuildingResult setActivePomProfiles( Model rawModel, List<Profile> activeProfiles )
    {
        if ( rawModel == null )
        {
            throw new IllegalArgumentException( "no model specified" );
        }

        if ( activeProfiles != null )
        {
            this.activePomProfiles.put( rawModel, new ArrayList<Profile>( activeProfiles ) );
        }
        else
        {
            this.activePomProfiles.remove( rawModel );
        }

        return this;
    }

    public DefaultModelBuildingResult addActivePomProfiles( Model rawModel, List<Profile> activeProfiles )
    {
        if ( rawModel == null )
        {
            throw new IllegalArgumentException( "no model specified" );
        }

        List<Profile> currentProfiles = this.activePomProfiles.get( rawModel );
        if ( currentProfiles == null )
        {
            currentProfiles = new ArrayList<Profile>( activeProfiles.size() );
            this.activePomProfiles.put( rawModel, currentProfiles );
        }
        currentProfiles.addAll( activeProfiles );

        return this;
    }

    public List<Profile> getActiveExternalProfiles()
    {
        return Collections.unmodifiableList( activeExternalProfiles );
    }

    public DefaultModelBuildingResult setActiveExternalProfiles( List<Profile> activeProfiles )
    {
        this.activeExternalProfiles.clear();
        if ( activeProfiles != null )
        {
            this.activeExternalProfiles.addAll( activeProfiles );
        }

        return this;
    }

}
