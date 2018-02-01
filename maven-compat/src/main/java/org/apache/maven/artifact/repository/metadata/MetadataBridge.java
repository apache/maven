package org.apache.maven.artifact.repository.metadata;

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
import java.util.Collections;
import java.util.Map;

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.metadata.AbstractMetadata;
import org.eclipse.aether.metadata.MergeableMetadata;
import org.eclipse.aether.metadata.Metadata;

/**
 * <strong>Warning:</strong> This is an internal utility class that is only public for technical reasons, it is not part
 * of the public API. In particular, this class can be changed or deleted without prior notice.
 *
 * @author Benjamin Bentmann
 */
public final class MetadataBridge
    extends AbstractMetadata
    implements MergeableMetadata
{

    private ArtifactMetadata metadata;

    private boolean merged;

    public MetadataBridge( ArtifactMetadata metadata )
    {
        this.metadata = metadata;
    }

    public void merge( File current, File result )
        throws RepositoryException
    {
        try
        {
            if ( current.exists() )
            {
                FileUtils.copyFile( current, result );
            }
            ArtifactRepository localRepo = new MetadataRepository( result );
            metadata.storeInLocalRepository( localRepo, localRepo );
            merged = true;
        }
        catch ( Exception e )
        {
            throw new RepositoryException( e.getMessage(), e );
        }
    }

    public boolean isMerged()
    {
        return merged;
    }

    public String getGroupId()
    {
        return emptify( metadata.getGroupId() );
    }

    public String getArtifactId()
    {
        return metadata.storedInGroupDirectory() ? "" : emptify( metadata.getArtifactId() );
    }

    public String getVersion()
    {
        return metadata.storedInArtifactVersionDirectory() ? emptify( metadata.getBaseVersion() ) : "";
    }

    public String getType()
    {
        return metadata.getRemoteFilename();
    }

    private String emptify( String string )
    {
        return ( string != null ) ? string : "";
    }

    public File getFile()
    {
        return null;
    }

    public MetadataBridge setFile( File file )
    {
        return this;
    }

    public Nature getNature()
    {
        if ( metadata instanceof RepositoryMetadata )
        {
            switch ( ( (RepositoryMetadata) metadata ).getNature() )
            {
                case RepositoryMetadata.RELEASE_OR_SNAPSHOT:
                    return Nature.RELEASE_OR_SNAPSHOT;
                case RepositoryMetadata.SNAPSHOT:
                    return Nature.SNAPSHOT;
                default:
                    return Nature.RELEASE;
            }
        }
        else
        {
            return Nature.RELEASE;
        }
    }

    public Map<String, String> getProperties()
    {
        return Collections.emptyMap();
    }

    @Override
    public Metadata setProperties( Map<String, String> properties )
    {
        return this;
    }

    @SuppressWarnings( "deprecation" )
    static class MetadataRepository
        extends DefaultArtifactRepository
    {

        private File metadataFile;

        MetadataRepository( File metadataFile )
        {
            super( "local", "", null );
            this.metadataFile = metadataFile;
        }

        @Override
        public String getBasedir()
        {
            return metadataFile.getParent();
        }

        @Override
        public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
        {
            return metadataFile.getName();
        }

    }

}
