package org.apache.maven.plugin.testing.stubs;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Stub class for {@link Artifact} testing.
 *
 * @author jesse
 * @version $Id$
 */
public class ArtifactStub
    implements Artifact
{
    private String groupId;

    private String artifactId;

    private String version;

    private String scope;

    private String type;

    private String classifier;

    private File file;

    private ArtifactRepository artifactRepository;

    /**
     * By default, return <code>0</code>
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object object )
    {
        return 0;
    }

    /** {@inheritDoc} */
    public String getGroupId()
    {
        return groupId;
    }

    /** {@inheritDoc} */
    public String getArtifactId()
    {
        return artifactId;
    }

    /** {@inheritDoc} */
    public String getVersion()
    {
        return version;
    }

    /** {@inheritDoc} */
    public void setVersion( String version )
    {
        this.version = version;
    }

    /** {@inheritDoc} */
    public String getScope()
    {
        return scope;
    }

    /** {@inheritDoc} */
    public String getType()
    {
        return type;
    }

    /**
     * Set a new type
     *
     * @param type
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /** {@inheritDoc} */
    public String getClassifier()
    {
        return classifier;
    }

    /** {@inheritDoc} */
    public boolean hasClassifier()
    {
        return classifier != null;
    }

    /** {@inheritDoc} */
    public File getFile()
    {
        return file;
    }

    /** {@inheritDoc} */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getBaseVersion()
     */
    public String getBaseVersion()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setBaseVersion(java.lang.String)
     */
    public void setBaseVersion( String string )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getId()
     */
    public String getId()
    {
        return null;
    }

    /**
     * @return <code>groupId:artifactId:type:classifier</code>.
     * @see org.apache.maven.artifact.Artifact#getDependencyConflictId()
     */
    public String getDependencyConflictId()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append( getGroupId() );
        buffer.append( ":" ).append( getArtifactId() );
        buffer.append( ":" ).append( getType() );
        buffer.append( ":" ).append( getClassifier() );

        return buffer.toString();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#addMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata)
     */
    public void addMetadata( ArtifactMetadata artifactMetadata )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getMetadataList()
     */
    public Collection getMetadataList()
    {
        return null;
    }

    /** {@inheritDoc} */
    public void setRepository( ArtifactRepository artifactRepository )
    {
        this.artifactRepository = artifactRepository;
    }

    /** {@inheritDoc} */
    public ArtifactRepository getRepository()
    {
        return artifactRepository;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#updateVersion(java.lang.String, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void updateVersion( String string, ArtifactRepository artifactRepository )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getDownloadUrl()
     */
    public String getDownloadUrl()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDownloadUrl(java.lang.String)
     */
    public void setDownloadUrl( String string )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getDependencyFilter()
     */
    public ArtifactFilter getDependencyFilter()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDependencyFilter(org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getArtifactHandler()
     */
    public ArtifactHandler getArtifactHandler()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getDependencyTrail()
     */
    public List getDependencyTrail()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDependencyTrail(java.util.List)
     */
    public void setDependencyTrail( List list )
    {
        // nop
    }

    /** {@inheritDoc} */
    public void setScope( String scope )
    {
        this.scope = scope;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getVersionRange()
     */
    public VersionRange getVersionRange()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setVersionRange(org.apache.maven.artifact.versioning.VersionRange)
     */
    public void setVersionRange( VersionRange versionRange )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#selectVersion(java.lang.String)
     */
    public void selectVersion( String string )
    {
        // nop
    }

    /** {@inheritDoc} */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /** {@inheritDoc} */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isSnapshot()
     */
    public boolean isSnapshot()
    {
        return Artifact.VERSION_FILE_PATTERN.matcher( getVersion() ).matches()
            || getVersion().endsWith( Artifact.SNAPSHOT_VERSION );
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setResolved(boolean)
     */
    public void setResolved( boolean b )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isResolved()
     */
    public boolean isResolved()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setResolvedVersion(java.lang.String)
     */
    public void setResolvedVersion( String string )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setArtifactHandler(org.apache.maven.artifact.handler.ArtifactHandler)
     */
    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isRelease()
     */
    public boolean isRelease()
    {
        return !isSnapshot();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setRelease(boolean)
     */
    public void setRelease( boolean b )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getAvailableVersions()
     */
    public List getAvailableVersions()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setAvailableVersions(java.util.List)
     */
    public void setAvailableVersions( List list )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isOptional()
     */
    public boolean isOptional()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @param b
     */
    public void setOptional( boolean b )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getSelectedVersion()
     */
    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return null;
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isSelectedVersionKnown()
     */
    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return false;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( getGroupId() != null )
        {
            sb.append( getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        if ( version != null )
        {
            sb.append( ":" );
            sb.append( getVersion() );
        }
        if ( scope != null )
        {
            sb.append( ":" );
            sb.append( scope );
        }
        return sb.toString();
    }

    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( getArtifactId() );
        sb.append( ":" );
        sb.append( getType() );
        if ( hasClassifier() )
        {
            sb.append( ":" );
            sb.append( getClassifier() );
        }
    }
}
