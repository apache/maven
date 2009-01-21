package org.apache.maven.mercury;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.ExclusionModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.profile.ProfileContext;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

/**
 * Provides a wrapper for the maven model.
 */
public final class MavenDomainModel
    implements DomainModel
{

    /**
     * Bytes containing the underlying model
     */
    private final List<ModelProperty> modelProperties;

    /**
     * History of joins and deletes of model properties
     */
    private String eventHistory;

    private ArtifactBasicMetadata parentMetadata;

    /**
     * Constructor
     * 
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel( byte[] bytes )
        throws IOException
    {
        this( new ByteArrayInputStream( bytes ) );
    }

    /**
     * Constructor
     * 
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel( InputStream inputStream )
        throws IOException
    {
        this( ModelMarshaller.marshallXmlToModelProperties( inputStream, ProjectUri.baseUri, PomTransformer.URIS ) );
    }

    /**
     * Constructor
     * 
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel( List<ModelProperty> modelProperties )
        throws IOException
    {
        if ( modelProperties == null )
        {
            throw new IllegalArgumentException( "modelProperties: null" );
        }

        this.modelProperties = new ArrayList<ModelProperty>( modelProperties );
    }

    public boolean hasParent()
    {
        // TODO: Expensive call if no parent
        return getParentMetadata() != null;
    }

    public List<ArtifactBasicMetadata> getDependencyMetadata()
        throws DataSourceException
    {
        List<ArtifactBasicMetadata> metadatas = new ArrayList<ArtifactBasicMetadata>();

        ModelDataSource source = new DefaultModelDataSource( modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );
        for ( ModelContainer modelContainer : source.queryFor( ProjectUri.Dependencies.Dependency.xUri ) )
        {
            metadatas.add( transformContainerToMetadata( modelContainer ) );
        }

        return metadatas;
    }

    public Collection<ModelContainer> getActiveProfileContainers( List<InterpolatorProperty> properties )
        throws DataSourceException
    {
        ModelDataSource dataSource = new DefaultModelDataSource( modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );

        return new ProfileContext( dataSource, properties ).getActiveProfiles();
    }

    public ArtifactBasicMetadata getParentMetadata()
    {
        if ( parentMetadata != null )
        {
            return copyArtifactBasicMetadata( parentMetadata );
        }

        String groupId = null, artifactId = null, version = null;

        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().equals( ProjectUri.Parent.version ) )
            {
                version = mp.getResolvedValue();
            }
            else if ( mp.getUri().equals( ProjectUri.Parent.artifactId ) )
            {
                artifactId = mp.getResolvedValue();
            }
            else if ( mp.getUri().equals( ProjectUri.Parent.groupId ) )
            {
                groupId = mp.getResolvedValue();
            }
            if ( groupId != null && artifactId != null && version != null )
            {
                break;
            }
        }

        if ( groupId == null || artifactId == null || version == null )
        {
            return null;
        }
        parentMetadata = new ArtifactBasicMetadata();
        parentMetadata.setArtifactId( artifactId );
        parentMetadata.setVersion( version );
        parentMetadata.setGroupId( groupId );

        return copyArtifactBasicMetadata( parentMetadata );
    }

    private ArtifactBasicMetadata copyArtifactBasicMetadata( ArtifactBasicMetadata metadata )
    {
        ArtifactBasicMetadata amd = new ArtifactBasicMetadata();
        amd.setArtifactId( metadata.getArtifactId() );
        amd.setGroupId( metadata.getGroupId() );
        amd.setVersion( metadata.getVersion() );
        return amd;
    }

    /**
     * @see org.apache.maven.shared.model.DomainModel#getEventHistory()
     */
    public String getEventHistory()
    {
        return eventHistory;
    }

    /**
     * @see org.apache.maven.shared.model.DomainModel#setEventHistory(String)
     */
    public void setEventHistory( String eventHistory )
    {
        if ( eventHistory == null )
        {
            throw new IllegalArgumentException( "eventHistory: null" );
        }
        this.eventHistory = eventHistory;
    }

    public List<ModelProperty> getModelProperties()
        throws IOException
    {
        return new ArrayList<ModelProperty>( modelProperties );
    }

    private ArtifactBasicMetadata transformContainerToMetadata( ModelContainer container )
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = container.getProperties();

        ArtifactBasicMetadata metadata = new ArtifactBasicMetadata();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.groupId ) )
            {
                metadata.setGroupId( mp.getResolvedValue() );
            }
            else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.artifactId ) )
            {
                metadata.setArtifactId( mp.getResolvedValue() );
            }
            else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.version ) )
            {
                metadata.setVersion( mp.getResolvedValue() );
            }
            else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.classifier ) )
            {
                metadata.setClassifier( mp.getResolvedValue() );
            }
            else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.scope ) )
            {
                metadata.setScope( mp.getResolvedValue() );
            }
            else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.type ) )
            {
                metadata.setType( mp.getResolvedValue() );
            }
            else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.optional ) )
            {
                metadata.setOptional( mp.getResolvedValue() );
            }
        }

        if ( metadata.getScope() == null )
        {
            metadata.setScope( "runtime" );
        }

        ModelDataSource dataSource = new DefaultModelDataSource( container.getProperties(), Arrays.asList( new ArtifactModelContainerFactory(),
                                                                   new ExclusionModelContainerFactory() ));
        List<ArtifactBasicMetadata> exclusions = new ArrayList<ArtifactBasicMetadata>();

        for ( ModelContainer exclusion : dataSource.queryFor( ProjectUri.Dependencies.Dependency.Exclusions.Exclusion.xUri ) )
        {
            ArtifactBasicMetadata meta = new ArtifactBasicMetadata();
            exclusions.add( meta );

            for ( ModelProperty mp : exclusion.getProperties() )
            {
                if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.Exclusions.Exclusion.artifactId ) )
                {
                    meta.setArtifactId( mp.getResolvedValue() );
                }
                else if ( mp.getUri().equals( ProjectUri.Dependencies.Dependency.Exclusions.Exclusion.groupId ) )
                {
                    meta.setGroupId( mp.getResolvedValue() );
                }
            }

        }
        metadata.setExclusions( exclusions );

        return metadata;
    }
}
