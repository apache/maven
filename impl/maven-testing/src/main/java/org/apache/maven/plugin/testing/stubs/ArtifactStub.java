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

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Stub class for {@link Artifact} testing.
 *
 * @author jesse
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
    @Override
    public int compareTo( Artifact artifact )
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupId()
    {
        return groupId;
    }

    /** {@inheritDoc} */
    @Override
    public String getArtifactId()
    {
        return artifactId;
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion()
    {
        return version;
    }

    /** {@inheritDoc} */
    @Override
    public void setVersion( String version )
    {
        this.version = version;
    }

    /** {@inheritDoc} */
    @Override
    public String getScope()
    {
        return scope;
    }

    /** {@inheritDoc} */
    @Override
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
    @Override
    public String getClassifier()
    {
        return classifier;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasClassifier()
    {
        return classifier != null;
    }

    /** {@inheritDoc} */
    @Override
    public File getFile()
    {
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getBaseVersion()
     */
    @Override
    public String getBaseVersion()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setBaseVersion(java.lang.String)
     */
    @Override
    public void setBaseVersion( String string )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getId()
     */
    @Override
    public String getId()
    {
        return null;
    }

    /**
     * @return <code>groupId:artifactId:type:classifier</code>.
     * @see org.apache.maven.artifact.Artifact#getDependencyConflictId()
     */
    @Override
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
    @Override
    public void addMetadata( ArtifactMetadata artifactMetadata )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getMetadataList()
     */
    @Override
    public Collection<ArtifactMetadata> getMetadataList()
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setRepository( ArtifactRepository artifactRepository )
    {
        this.artifactRepository = artifactRepository;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactRepository getRepository()
    {
        return artifactRepository;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#updateVersion(java.lang.String, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    @Override
    public void updateVersion( String string, ArtifactRepository artifactRepository )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getDownloadUrl()
     */
    @Override
    public String getDownloadUrl()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDownloadUrl(java.lang.String)
     */
    @Override
    public void setDownloadUrl( String string )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getDependencyFilter()
     */
    @Override
    public ArtifactFilter getDependencyFilter()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDependencyFilter(org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    @Override
    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getArtifactHandler()
     */
    @Override
    public ArtifactHandler getArtifactHandler()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getDependencyTrail()
     */
    @Override
    public List<String> getDependencyTrail()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setDependencyTrail(java.util.List)
     */
    @Override
    public void setDependencyTrail( List<String> list )
    {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void setScope( String scope )
    {
        this.scope = scope;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getVersionRange()
     */
    @Override
    public VersionRange getVersionRange()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setVersionRange(org.apache.maven.artifact.versioning.VersionRange)
     */
    @Override
    public void setVersionRange( VersionRange versionRange )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#selectVersion(java.lang.String)
     */
    @Override
    public void selectVersion( String string )
    {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isSnapshot()
     */
    @Override
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
    @Override
    public void setResolved( boolean b )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isResolved()
     */
    @Override
    public boolean isResolved()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setResolvedVersion(java.lang.String)
     */
    @Override
    public void setResolvedVersion( String string )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setArtifactHandler(org.apache.maven.artifact.handler.ArtifactHandler)
     */
    @Override
    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isRelease()
     */
    @Override
    public boolean isRelease()
    {
        return !isSnapshot();
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setRelease(boolean)
     */
    @Override
    public void setRelease( boolean b )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getAvailableVersions()
     */
    @Override
    public List<ArtifactVersion> getAvailableVersions()
    {
        return null;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.Artifact#setAvailableVersions(java.util.List)
     */
    @Override
    public void setAvailableVersions( List<ArtifactVersion> list )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isOptional()
     */
    @Override
    public boolean isOptional()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @param b
     */
    @Override
    public void setOptional( boolean b )
    {
        // nop
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.Artifact#getSelectedVersion()
     */
    @Override
    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return null;
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.Artifact#isSelectedVersionKnown()
     */
    @Override
    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return false;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
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

    public boolean isFromAuthoritativeRepository()
    {
        return true;
    }

    public void setFromAuthoritativeRepository( boolean fromAuthoritativeRepository )
    {
        // nothing
    }
}
