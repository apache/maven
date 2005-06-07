package org.apache.maven.artifact;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 * @todo this should possibly be replaced by type handler
 */
public class DefaultArtifact
    implements Artifact
{
    private final String groupId;

    private final String artifactId;

    private String version;

    // TODO: should be final
    private String baseVersion;

    private final String type;

    private final String classifier;

    private final String scope;

    private List metadataList;

    private File file;

    private ArtifactRepository repository;

    private String downloadUrl;

    private ArtifactFilter dependencyFilter;

    /**
     * !!! WARNING !!! Never put <classifier/> in the POM. It is for mojo use
     * only. Classifier is for specifying derived artifacts, like ejb-client.
     */
    public DefaultArtifact( String groupId,
                            String artifactId,
                            String version,
                            String scope,
                            String type,
                            String classifier )
    {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.version = version;

        this.type = type;

        this.scope = scope;

        this.classifier = classifier;
        
        validateIdentity();
    }

    private void validateIdentity()
    {
        if( empty( groupId ) )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, version, type, "The groupId cannot be empty." );
        }

        if( artifactId == null )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, version, type, "The artifactId cannot be empty." );
        }

        if ( type == null )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, version, type, "The type cannot be empty." );
        }

        if( version == null )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, version, type, "The version cannot be empty." );
        }
    }

    private boolean empty( String value )
    {
        return value == null || value.trim().length() < 1;
    }

    public DefaultArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        this( groupId, artifactId, version, scope, type, null );
    }

    public DefaultArtifact( String groupId, String artifactId, String version, String type )
    {
        this( groupId, artifactId, version, null, type, null );
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean hasClassifier()
    {
        return StringUtils.isNotEmpty( classifier );
    }

    public String getScope()
    {
        return scope;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getType()
    {
        return type;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getFile()
    {
        return file;
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    public void setRepository( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getId()
    {
        return getConflictId() + ( hasClassifier() ? ( ":" + getClassifier() ) : "" ) + ":" + getBaseVersion();
    }

    public String getConflictId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getType();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        if ( metadataList == null )
        {
            metadataList = new ArrayList();
        }
        metadataList.add( metadata );
    }

    public List getMetadataList()
    {
        return metadataList == null ? Collections.EMPTY_LIST : metadataList;
    }

    // ----------------------------------------------------------------------
    // Object overrides
    // ----------------------------------------------------------------------

    public String toString()
    {
        return getId();
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + groupId.hashCode();
        result = 37 * result + artifactId.hashCode();
        result = 37 * result + type.hashCode();
        result = 37 * result + version.hashCode();
        result = 37 * result + ( classifier != null ? classifier.hashCode() : 0 );
        return result;
    }

    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof Artifact ) )
        {
            return false;
        }

        Artifact a = (Artifact) o;

        if ( !a.getGroupId().equals( groupId ) )
        {
            return false;
        }
        else if ( !a.getArtifactId().equals( artifactId ) )
        {
            return false;
        }
        else if ( !a.getVersion().equals( version ) )
        {
            return false;
        }
        else if ( !a.getType().equals( type ) )
        {
            return false;
        }
        else if ( classifier == null ? a.getClassifier() != null : !a.getClassifier().equals( classifier ) )
        {
            return false;
        }
        return true;
    }

    public String getBaseVersion()
    {
        if ( baseVersion == null )
        {
            baseVersion = version;

            if ( version == null )
            {
                throw new NullPointerException( "version was null for " + groupId + ":" + artifactId );
            }
        }
        return baseVersion;
    }

    public void setBaseVersion( String baseVersion )
    {
        this.baseVersion = baseVersion;
    }

    public int compareTo( Object o )
    {
        Artifact a = (Artifact) o;

        int result = groupId.compareTo( a.getGroupId() );
        if ( result == 0 )
        {
            result = artifactId.compareTo( a.getArtifactId() );
            if ( result == 0 )
            {
                result = type.compareTo( a.getType() );
                if ( result == 0 )
                {
                    if ( classifier == null )
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = 1;
                        }
                    }
                    else
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = classifier.compareTo( a.getClassifier() );
                        }
                        else
                        {
                            result = -1;
                        }
                    }
                    if ( result == 0 )
                    {
                        result = version.compareTo( a.getVersion() );
                    }
                }
            }
        }
        return result;
    }

    public void updateVersion( String version, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        setVersion( version );
        try
        {
            setFile( new File( localRepository.getBasedir(), localRepository.pathOf( this ) ) );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
        }
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl( String downloadUrl )
    {
        this.downloadUrl = downloadUrl;
    }

    public ArtifactFilter getDependencyFilter()
    {
        return dependencyFilter;
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        this.dependencyFilter = artifactFilter;
    }
}
