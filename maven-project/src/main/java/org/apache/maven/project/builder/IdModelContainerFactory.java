package org.apache.maven.project.builder;

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

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class IdModelContainerFactory
    implements ModelContainerFactory
{

    private static final Collection<String> uris = Collections.unmodifiableList( Arrays.asList(
        ProjectUri.PluginRepositories.PluginRepository.xUri, ProjectUri.Repositories.Repository.xUri,
        ProjectUri.Reporting.Plugins.Plugin.ReportSets.ReportSet.xUri, ProjectUri.Profiles.Profile.xUri,
        ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri ) );

    public Collection<String> getUris()
    {
        return uris;
    }

    public ModelContainer create( List<ModelProperty> modelProperties )
    {
        if ( modelProperties == null || modelProperties.size() == 0 )
        {
            throw new IllegalArgumentException( "modelProperties: null or empty" );
        }
        return new IdModelContainer( modelProperties );
    }

    private static class IdModelContainer
        implements ModelContainer
    {

        private String id;

        private List<ModelProperty> properties;

        private IdModelContainer( List<ModelProperty> properties )
        {
            this.properties = new ArrayList<ModelProperty>( properties );
            this.properties = Collections.unmodifiableList( this.properties );

            for ( ModelProperty mp : properties )
            {
                if ( mp.getUri().endsWith( "/id" ) )
                {
                    this.id = mp.getValue();
                }
            }
        }

        public ModelContainerAction containerAction( ModelContainer modelContainer )
        {
            if ( modelContainer == null )
            {
                throw new IllegalArgumentException( "modelContainer: null" );
            }

            if ( !( modelContainer instanceof IdModelContainer ) )
            {
                throw new IllegalArgumentException( "modelContainer: wrong type" );
            }

            IdModelContainer c = (IdModelContainer) modelContainer;
            if ( c.id == null || id == null )
            {
                return ModelContainerAction.NOP;
            }
            return ( c.id.equals( id ) ) ? ModelContainerAction.JOIN : ModelContainerAction.NOP;
        }

        public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
        {
            return new IdModelContainer( modelProperties );
        }

        public List<ModelProperty> getProperties()
        {
            return properties;
        }

        public String toString()
        {
            return "ID = " + id;
        }
    }
}
