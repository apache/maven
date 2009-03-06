package org.apache.maven.project.builder.factories;

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
import org.apache.maven.project.builder.ProjectUri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ArtifactModelContainerFactory
    implements ModelContainerFactory
{

    private static final Collection<String> uris = Collections.unmodifiableList( Arrays.asList(
        ProjectUri.DependencyManagement.Dependencies.Dependency.xUri,
        ProjectUri.Dependencies.Dependency.xUri,
        ProjectUri.Reporting.Plugins.Plugin.xUri,
        ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri,
        ProjectUri.Build.Plugins.Plugin.xUri,
        ProjectUri.Build.Extensions.Extension.xUri    
         ) );

    private final Collection<String> u;

    public Collection<String> getUris()
    {
        return u;
    }

    public ArtifactModelContainerFactory() {
        u = uris;
    }

    public ArtifactModelContainerFactory(String uri) {
        u = Collections.unmodifiableList( Arrays.asList(uri) );
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

        private String scope;

        private String classifier;
        
        private String uri;

        private List<ModelProperty> properties;

        private static String findBaseUriFrom( List<ModelProperty> modelProperties )
        {
            String baseUri = null;
            for ( ModelProperty mp : modelProperties )
            {
                if ( baseUri == null || mp.getUri().length() < baseUri.length() )
                {
                    baseUri = mp.getUri();
                }
            }
            return baseUri;
        }

        private ArtifactModelContainer( List<ModelProperty> properties )
        {
            this.properties = new ArrayList<ModelProperty>( properties );
            this.properties = Collections.unmodifiableList( this.properties );
            uri = findBaseUriFrom( this.properties );

            for ( ModelProperty mp : this.properties )
            {
                if ( version == null && mp.getUri().equals( uri + "/version" ) )
                {
                    this.version = mp.getResolvedValue();
                }
                else if ( artifactId == null && mp.getUri().equals( uri + "/artifactId" ) )
                {
                    this.artifactId = mp.getResolvedValue();
                }
                else if ( groupId == null && mp.getUri().equals( uri + "/groupId" ) )
                {
                    this.groupId = mp.getResolvedValue();
                }
                else if ( scope == null && mp.getUri().equals( uri + "/scope" ) )
                {
                    this.scope = mp.getResolvedValue();
                }
                else if ( classifier == null && mp.getUri().equals( uri + "/classifier" ) )
                {
                    this.classifier = mp.getResolvedValue();
                }
                else if ( type == null && mp.getUri().equals( ProjectUri.Dependencies.Dependency.type )
                        || mp.getUri().equals(ProjectUri.DependencyManagement.Dependencies.Dependency.type)
                        || mp.getUri().equals(ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.Dependency.type)
                        || mp.getUri().equals(ProjectUri.Build.Plugins.Plugin.Dependencies.Dependency.type))
                {
                    this.type = mp.getResolvedValue();
                }
            }
            if ( groupId == null )
            {
                if ( ProjectUri.Build.Plugins.Plugin.xUri.equals( uri )
                    || ProjectUri.Profiles.Profile.Build.Plugins.Plugin.xUri.equals( uri )
                    || ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri.equals( uri )
                    || ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.Plugin.xUri.equals( uri )
                    || ProjectUri.Reporting.Plugins.Plugin.xUri.equals( uri )
                    || ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.xUri.equals( uri ))
                {
                    groupId = "org.apache.maven.plugins";
                }
                else
                {
                	for(ModelProperty mp1 : properties)
                	{
                		System.out.println("----" + mp1);
                	}
                    throw new IllegalArgumentException( "Properties do not contain group id. Artifact ID = "
                        + artifactId + ", Version = " + version );
                }
            }

            if ( artifactId == null )
            {
                StringBuffer sb = new StringBuffer();
                for ( ModelProperty mp : properties )
                {
                    sb.append( mp ).append( "\r\n" );
                }
                throw new IllegalArgumentException( "Properties does not contain artifact id. Group ID = " + groupId +
                    ", Version = " + version + ", Base = " + uri + ":\r\n" + sb );
            }

            if ( version == null )
            {
                version = "";
            }

            if ( type == null )
            {
                type = "jar";
            }

            if ( classifier == null )
            {
                classifier = "";
            }

            if ( scope == null || scope.equals("provided"))
            {
                scope = "compile";
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
            if ( c.groupId.equals( groupId ) && c.artifactId.equals( artifactId ) && c.type.equals( type )
                    && c.classifier.equals( classifier ))
            {
                if ( uri.startsWith(ProjectUri.Build.Plugins.xUri) || c.version.equals( version ) 
                		|| version.equals("") || c.version.equals(""))
                {
                    return ModelContainerAction.JOIN;
                }
                else
                {
                    return ModelContainerAction.DELETE;
                }
            }
            return ModelContainerAction.NOP;
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
