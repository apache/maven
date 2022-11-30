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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.Proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class StubArtifactRepository
    implements ArtifactRepository
{
    private String baseDir = null;

    /**
     * Default constructor
     *
     * @param dir the basedir
     */
    public StubArtifactRepository( String dir )
    {
        baseDir = dir;
    }

    /**
     * @return the <code>artifactId</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOf(org.apache.maven.artifact.Artifact)
     */
    @Override
    public String pathOf( Artifact artifact )
    {
        return artifact.getId();
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOfRemoteRepositoryMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata)
     */
    @Override
    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        return null;
    }

    /**
     * @return the filename of this metadata on the local repository.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOfLocalRepositoryMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    @Override
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return metadata.getLocalFilename( repository );
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getUrl()
     */
    @Override
    public String getUrl()
    {
        return null;
    }

    /**
     * @return <code>basedir</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getBasedir()
     */
    @Override
    public String getBasedir()
    {
        return baseDir;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getProtocol()
     */
    @Override
    public String getProtocol()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getId()
     */
    @Override
    public String getId()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getSnapshots()
     */
    @Override
    public ArtifactRepositoryPolicy getSnapshots()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getReleases()
     */
    @Override
    public ArtifactRepositoryPolicy getReleases()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getLayout()
     */
    @Override
    public ArtifactRepositoryLayout getLayout()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getKey()
     */
    @Override
    public String getKey()
    {
        return null;
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#isUniqueVersion()
     */
    @Override
    public boolean isUniqueVersion()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.repository.ArtifactRepository#setBlacklisted(boolean)
     */
    @Override
    public void setBlacklisted( boolean blackListed )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#isBlacklisted()
     */
    @Override
    public boolean isBlacklisted()
    {
        return false;
    }

    @Override
    public Artifact find( Artifact artifact )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Authentication getAuthentication()
    {
        return null;
    }

    @Override
    public Proxy getProxy()
    {
        return null;
    }

    @Override
    public void setAuthentication( Authentication authentication )
    {

    }

    @Override
    public void setId( String id )
    {

    }

    @Override
    public void setLayout( ArtifactRepositoryLayout layout )
    {

    }

    @Override
    public void setProxy( Proxy proxy )
    {

    }

    @Override
    public void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy )
    {

    }

    @Override
    public void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy )
    {

    }

    @Override
    public void setUrl( String url )
    {

    }

    @Override
    public List<String> findVersions( Artifact artifact )
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isProjectAware()
    {
        return false;
    }

    @Override
    public List<ArtifactRepository> getMirroredRepositories()
    {
        return new ArrayList<>( 0 );
    }

    @Override
    public void setMirroredRepositories( List<ArtifactRepository> artifactRepositories )
    {
        // no op
    }

    @Override
    public boolean isBlocked()
    {
        return false;
    }

    @Override
    public void setBlocked( boolean blocked )
    {
        // no op
    }
}
