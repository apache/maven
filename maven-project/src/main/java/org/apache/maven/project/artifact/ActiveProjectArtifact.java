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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Wraps an active project instance to be able to receive updates from its artifact without affecting the original
 * attributes of this artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo I think this exposes a design flaw in that the immutable and mutable parts of an artifact are in one class and
 * should be split. ie scope, file, etc depend on the context of use, whereas everything else is immutable.
 */
public class ActiveProjectArtifact
    implements Artifact
{
    private final Artifact artifact;

    private final MavenProject project;

    public ActiveProjectArtifact( MavenProject project, Artifact artifact )
    {
        this.artifact = artifact;
        this.project = project;

        artifact.setFile( project.getArtifact().getFile() );
        artifact.setResolved( true );
    }

    public File getFile()
    {
        // we need to get the latest file for the project, not the artifact that was created at one point in time
        return project.getArtifact().getFile();
    }

    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    public String getVersion()
    {
        return artifact.getVersion();
    }

    public void setVersion( String version )
    {
        artifact.setVersion( version );
    }

    public String getScope()
    {
        return artifact.getScope();
    }

    public String getType()
    {
        return artifact.getType();
    }

    public String getClassifier()
    {
        return artifact.getClassifier();
    }

    public boolean hasClassifier()
    {
        return artifact.hasClassifier();
    }

    public void setFile( File destination )
    {
        artifact.setFile( destination );
        project.getArtifact().setFile( destination );
    }

    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }

    public void setBaseVersion( String baseVersion )
    {
        artifact.setBaseVersion( baseVersion );
    }

    public String getId()
    {
        return artifact.getId();
    }

    public String getDependencyConflictId()
    {
        return artifact.getDependencyConflictId();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        artifact.addMetadata( metadata );
    }

    public Collection getMetadataList()
    {
        return artifact.getMetadataList();
    }

    public void setRepository( ArtifactRepository remoteRepository )
    {
        artifact.setRepository( remoteRepository );
    }

    public ArtifactRepository getRepository()
    {
        return artifact.getRepository();
    }

    public void updateVersion( String version, ArtifactRepository localRepository )
    {
        artifact.updateVersion( version, localRepository );
    }

    public String getDownloadUrl()
    {
        return artifact.getDownloadUrl();
    }

    public void setDownloadUrl( String downloadUrl )
    {
        artifact.setDownloadUrl( downloadUrl );
    }

    public ArtifactFilter getDependencyFilter()
    {
        return artifact.getDependencyFilter();
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        artifact.setDependencyFilter( artifactFilter );
    }

    public ArtifactHandler getArtifactHandler()
    {
        return artifact.getArtifactHandler();
    }

    public List getDependencyTrail()
    {
        return artifact.getDependencyTrail();
    }

    public void setDependencyTrail( List dependencyTrail )
    {
        artifact.setDependencyTrail( dependencyTrail );
    }

    public void setScope( String scope )
    {
        artifact.setScope( scope );
    }

    public VersionRange getVersionRange()
    {
        return artifact.getVersionRange();
    }

    public void setVersionRange( VersionRange newRange )
    {
        artifact.setVersionRange( newRange );
    }

    public void selectVersion( String version )
    {
        artifact.selectVersion( version );
    }

    public void setGroupId( String groupId )
    {
        artifact.setGroupId( groupId );
    }

    public void setArtifactId( String artifactId )
    {
        artifact.setArtifactId( artifactId );
    }

    public boolean isSnapshot()
    {
        return artifact.isSnapshot();
    }

    public int compareTo( Object o )
    {
        return artifact.compareTo( o );
    }

    public void setResolved( boolean resolved )
    {
        artifact.setResolved( resolved );
    }

    public boolean isResolved()
    {
        return artifact.isResolved();
    }

    public void setResolvedVersion( String version )
    {
        artifact.setResolvedVersion( version );
    }

    public void setArtifactHandler( ArtifactHandler handler )
    {
        artifact.setArtifactHandler( handler );
    }

    public String toString()
    {
        return "active project artifact:\n\tartifact = " + artifact + ";\n\tproject: " + project;
    }

    public boolean isRelease()
    {
        return artifact.isRelease();
    }

    public void setRelease( boolean release )
    {
        artifact.setRelease( release );
    }

    public List getAvailableVersions()
    {
        return artifact.getAvailableVersions();
    }

    public void setAvailableVersions( List versions )
    {
        artifact.setAvailableVersions( versions );
    }

    public boolean isOptional()
    {
        return artifact.isOptional();
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return artifact.getSelectedVersion();
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return artifact.isSelectedVersionKnown();
    }

    public void setOptional( boolean optional )
    {
        artifact.setOptional( optional );
    }
}
