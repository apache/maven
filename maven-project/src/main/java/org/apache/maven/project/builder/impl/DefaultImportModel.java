package org.apache.maven.project.builder.impl;

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

import org.apache.maven.shared.model.ImportModel;
import org.apache.maven.shared.model.ModelProperty;

import java.util.ArrayList;
import java.util.List;

public final class DefaultImportModel
    implements ImportModel
{

    private final String id;

    private final List<ModelProperty> modelProperties;

    public DefaultImportModel( String id, List<ModelProperty> modelProperties )
    {
        if ( id == null )
        {
            throw new IllegalArgumentException( "id: null" );
        }

        if ( modelProperties == null )
        {
            throw new IllegalArgumentException( "modelProperties: null" );
        }
        this.id = id;
        this.modelProperties = new ArrayList<ModelProperty>( modelProperties );
    }

    public String getId()
    {
        return id;
    }

    public List<ModelProperty> getModelProperties()
    {
        return new ArrayList<ModelProperty>( modelProperties );
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ImportModel that = (ImportModel) o;

        if ( id != null ? !id.equals( that.getId() ) : that.getId() != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return ( id != null ? id.hashCode() : 0 );
    }
}
