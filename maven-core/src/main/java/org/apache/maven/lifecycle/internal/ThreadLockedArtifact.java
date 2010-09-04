package org.apache.maven.lifecycle.internal;

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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An artifact that conditionally suspends on getFile for anything but the thread it is locked to.
 * 
 * @since 3.0
 */
class ThreadLockedArtifact
    implements Artifact
{
    private final Artifact real;

    private final CountDownLatch artifactLocked = new CountDownLatch( 1 );

    ThreadLockedArtifact( Artifact real )
    {
        this.real = real;
    }

    public boolean hasReal()
    {
        return real != null &&
            ( !( real instanceof ThreadLockedArtifact ) || ( (ThreadLockedArtifact) real ).hasReal() );
    }

    public String getGroupId()
    {
        return real.getGroupId();
    }

    public String getArtifactId()
    {
        return real.getArtifactId();
    }

    public String getVersion()
    {
        return real.getVersion();
    }

    public void setVersion( String version )
    {
        real.setVersion( version );
    }

    public String getScope()
    {
        return real.getScope();
    }

    public String getType()
    {
        return real.getType();
    }

    public String getClassifier()
    {
        return real.getClassifier();
    }

    public boolean hasClassifier()
    {
        return real.hasClassifier();
    }

    private static final InheritableThreadLocal<ThreadLockedArtifact> threadArtifact =
        new InheritableThreadLocal<ThreadLockedArtifact>();

    public void attachToThread()
    {
        threadArtifact.set( this );
    }

    public File getFile()
    {
        final ThreadLockedArtifact lockedArtifact = threadArtifact.get();
        if ( lockedArtifact != null && this != lockedArtifact && mustLock() )
        {
            try
            {
                artifactLocked.await();
            }
            catch ( InterruptedException e )
            {
                // Ignore and go on to real.getFile();
            }
        }
        return real.getFile();
    }

    private boolean mustLock()
    {
        boolean dontNeedLock = CurrentPhaseForThread.isPhase( "compile" ) || CurrentPhaseForThread.isPhase( "test" );
        return !dontNeedLock;
    }

    public void setFile( File destination )
    {
        if ( destination != null && destination.exists() && destination.isFile() )
        {
            artifactLocked.countDown();
        }
        real.setFile( destination );
    }

    public String getBaseVersion()
    {
        return real.getBaseVersion();
    }

    public void setBaseVersion( String baseVersion )
    {
        real.setBaseVersion( baseVersion );
    }

    public String getId()
    {
        return real.getId();
    }

    public String getDependencyConflictId()
    {
        return real.getDependencyConflictId();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        real.addMetadata( metadata );
    }

    public Collection<ArtifactMetadata> getMetadataList()
    {
        return real.getMetadataList();
    }

    public void setRepository( ArtifactRepository remoteRepository )
    {
        real.setRepository( remoteRepository );
    }

    public ArtifactRepository getRepository()
    {
        return real.getRepository();
    }

    public void updateVersion( String version, ArtifactRepository localRepository )
    {
        real.updateVersion( version, localRepository );
    }

    public String getDownloadUrl()
    {
        return real.getDownloadUrl();
    }

    public void setDownloadUrl( String downloadUrl )
    {
        real.setDownloadUrl( downloadUrl );
    }

    public ArtifactFilter getDependencyFilter()
    {
        return real.getDependencyFilter();
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        real.setDependencyFilter( artifactFilter );
    }

    public ArtifactHandler getArtifactHandler()
    {
        return real.getArtifactHandler();
    }

    public List<String> getDependencyTrail()
    {
        return real.getDependencyTrail();
    }

    public void setDependencyTrail( List<String> dependencyTrail )
    {
        real.setDependencyTrail( dependencyTrail );
    }

    public void setScope( String scope )
    {
        real.setScope( scope );
    }

    public VersionRange getVersionRange()
    {
        return real.getVersionRange();
    }

    public void setVersionRange( VersionRange newRange )
    {
        real.setVersionRange( newRange );
    }

    public void selectVersion( String version )
    {
        real.selectVersion( version );
    }

    public void setGroupId( String groupId )
    {
        real.setGroupId( groupId );
    }

    public void setArtifactId( String artifactId )
    {
        real.setArtifactId( artifactId );
    }

    public boolean isSnapshot()
    {
        return real.isSnapshot();
    }

    public void setResolved( boolean resolved )
    {
        real.setResolved( resolved );
    }

    public boolean isResolved()
    {
        return real.isResolved();
    }

    public void setResolvedVersion( String version )
    {
        real.setResolvedVersion( version );
    }

    public void setArtifactHandler( ArtifactHandler handler )
    {
        real.setArtifactHandler( handler );
    }

    public boolean isRelease()
    {
        return real.isRelease();
    }

    public void setRelease( boolean release )
    {
        real.setRelease( release );
    }

    public List<ArtifactVersion> getAvailableVersions()
    {
        return real.getAvailableVersions();
    }

    public void setAvailableVersions( List<ArtifactVersion> versions )
    {
        real.setAvailableVersions( versions );
    }

    public boolean isOptional()
    {
        return real.isOptional();
    }

    public void setOptional( boolean optional )
    {
        real.setOptional( optional );
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return real.getSelectedVersion();
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return real.isSelectedVersionKnown();
    }

    public int compareTo( Artifact o )
    {
        return real.compareTo( o );
    }
}
