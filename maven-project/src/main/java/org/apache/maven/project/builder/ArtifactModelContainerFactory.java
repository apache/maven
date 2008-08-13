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

public final class ArtifactModelContainerFactory
    implements ModelContainerFactory
{

    private static final Collection<String> uris = Collections.unmodifiableList( Arrays.asList(

        ProjectUri.DependencyManagement.Dependencies.Dependency.xUri, ProjectUri.Dependencies.Dependency.xUri,

        ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri,
        ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.Dependency.xUri,

        ProjectUri.Build.Plugins.Plugin.xUri, ProjectUri.Build.Plugins.Plugin.Dependencies.Dependency.xUri,
        ProjectUri.Build.Plugins.Plugin.Dependencies.Dependency.Exclusions.Exclusion.xUri ) );

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
        return new ArtifactModelContainer( modelProperties );
    }

    private static class ArtifactModelContainer
        implements ModelContainer
    {

        private String groupId;

        private String artifactId;

        private String version;

        private String type;

        private List<ModelProperty> properties;

        private ArtifactModelContainer( List<ModelProperty> properties )
        {
            this.properties = new ArrayList<ModelProperty>( properties );
            this.properties = Collections.unmodifiableList( this.properties );

            for ( ModelProperty mp : properties )
            {
                if ( mp.getUri().endsWith( "version" ) )
                {
                    this.version = mp.getValue();
                }
                else if ( mp.getUri().endsWith( "artifactId" ) )
                {
                    this.artifactId = mp.getValue();
                }
                else if ( mp.getUri().endsWith( "groupId" ) )
                {
                    this.groupId = mp.getValue();
                }
                else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.type ) )
                {
                    this.type = mp.getValue();
                }
            }
            if ( groupId == null )
            {
                groupId = "org.apache.maven.plugins";
                //  throw new IllegalArgumentException("properties does not contain group id. Artifact ID = "
                //          + artifactId + ", Version = " + version);
            }

            if ( artifactId == null )
            {
                throw new IllegalArgumentException(
                    "Properties does not contain artifact id. Group ID = " + groupId + ", Version = " + version );
            }

            if ( type == null )
            {
                type = "";
            }
        }

        public ModelContainerAction containerAction( ModelContainer modelContainer )
        {
            if ( modelContainer == null )
            {
                throw new IllegalArgumentException( "modelContainer: null" );
            }

            if ( !( modelContainer instanceof ArtifactModelContainer ) )
            {
                throw new IllegalArgumentException( "modelContainer: wrong type" );
            }

            ArtifactModelContainer c = (ArtifactModelContainer) modelContainer;
            if ( c.groupId.equals( groupId ) && c.artifactId.equals( artifactId ) )
            {
                if ( c.version == null )
                {
                    if ( version == null )
                    {
                        return ModelContainerAction.JOIN;
                    }
                    return ModelContainerAction.DELETE;//TODO Verify - PluginManagement Section may make versions equal
                }

                if ( c.version.equals( version ) )
                {
                    if ( c.type.equals( type ) )
                    {
                        return ModelContainerAction.JOIN;
                    }
                    else
                    {
                        return ModelContainerAction.NOP;
                    }
                }
                else
                {
                    return ModelContainerAction.DELETE;
                }
            }
            else
            {
                return ModelContainerAction.NOP;
            }
        }

        public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
        {
            return new ArtifactModelContainer( modelProperties );
        }

        public List<ModelProperty> getProperties()
        {
            return properties;
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append( "Group ID = " ).append( groupId ).append( ", Artifact ID = " ).append( artifactId )
                .append( ", Version" ).append( version ).append( "\r\n" );
            for ( ModelProperty mp : properties )
            {
                sb.append( mp ).append( "\r\n" );
            }
            return sb.toString();
        }
    }
}
