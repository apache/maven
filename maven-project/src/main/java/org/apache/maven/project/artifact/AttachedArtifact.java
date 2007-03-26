package org.apache.maven.project.artifact;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AttachedArtifact
    extends DefaultArtifact
{

    private final Artifact parent;

    public AttachedArtifact( Artifact parent, String type, String classifier, ArtifactHandler artifactHandler )
    {        
        super( parent.getGroupId(), parent.getArtifactId(), parent.getVersionRange(), parent.getScope(), type,
               classifier, artifactHandler, parent.isOptional() );
        
        setDependencyTrail( Collections.singletonList( parent.getId() ) );
        
        this.parent = parent;
        
        if ( getId().equals( parent.getId() ) )
        {
            throw new InvalidArtifactRTException( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), parent.getType(), "An attached artifact must have a different ID than its corresponding main artifact." );
        }
    }

    public AttachedArtifact( Artifact parent, String type, ArtifactHandler artifactHandler )
    {
        this( parent, type, null, artifactHandler );
    }
    
    public void setArtifactId( String artifactId )
    {
        throw new UnsupportedOperationException( "Cannot change the artifactId for an attached artifact.  It is derived from the main artifact." );
    }

    public List getAvailableVersions()
    {
        return parent.getAvailableVersions();
    }
    
    public void setAvailableVersions( List availableVersions )
    {
        throw new UnsupportedOperationException( "Cannot change the version information for an attached artifact. It is derived from the main artifact." );
    }

    public String getBaseVersion()
    {
        return parent.getBaseVersion();
    }
    
    public void setBaseVersion( String baseVersion )
    {
        throw new UnsupportedOperationException( "Cannot change the version information for an attached artifact. It is derived from the main artifact." );
    }

    public String getDownloadUrl()
    {
        return parent.getDownloadUrl();
    }
    
    public void setDownloadUrl( String downloadUrl )
    {
        throw new UnsupportedOperationException( "Cannot change the download information for an attached artifact. It is derived from the main artifact." );
    }

    public void setGroupId( String groupId )
    {
        throw new UnsupportedOperationException( "Cannot change the groupId on attached artifacts. It is derived from the main artifact." );
    }

    public ArtifactRepository getRepository()
    {
        return parent.getRepository();
    }
    
    public void setRepository( ArtifactRepository repository )
    {
        throw new UnsupportedOperationException( "Cannot change the repository information for an attached artifact. It is derived from the main artifact." );
    }

    public String getScope()
    {
        return parent.getScope();
    }
    
    public void setScope( String scope )
    {
        throw new UnsupportedOperationException( "Cannot change the scoping information for an attached artifact. It is derived from the main artifact." );
    }

    public String getVersion()
    {
        return parent.getVersion();
    }
    
    public void setVersion( String version )
    {
        throw new UnsupportedOperationException( "Cannot change the version information for an attached artifact. It is derived from the main artifact." );
    }

    public VersionRange getVersionRange()
    {
        return parent.getVersionRange();
    }
    
    public void setVersionRange( VersionRange range )
    {
        throw new UnsupportedOperationException( "Cannot change the version information for an attached artifact. It is derived from the main artifact." );
    }

    public boolean isRelease()
    {
        return parent.isRelease();
    }
    
    public void setRelease( boolean release )
    {
        throw new UnsupportedOperationException( "Cannot change the version information for an attached artifact. It is derived from the main artifact." );
    }

    public boolean isSnapshot()
    {
        return parent.isSnapshot();
    }
    
    public void addMetadata( ArtifactMetadata metadata )
    {
        // ignore. The parent artifact will handle metadata.
        // we must fail silently here to avoid problems with the artifact transformers.
    }

    public Collection getMetadataList()
    {
        return Collections.EMPTY_LIST;
    }

}
