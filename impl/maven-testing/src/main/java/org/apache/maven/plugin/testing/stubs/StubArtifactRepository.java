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
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
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
    public String pathOf( Artifact artifact )
    {
        return artifact.getId();
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOfRemoteRepositoryMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata)
     */
    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        return null;
    }

    /**
     * @return the filename of this metadata on the local repository.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#pathOfLocalRepositoryMetadata(org.apache.maven.artifact.metadata.ArtifactMetadata, org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return metadata.getLocalFilename( repository );
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getUrl()
     */
    public String getUrl()
    {
        return null;
    }

    /**
     * @return <code>basedir</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getBasedir()
     */
    public String getBasedir()
    {
        return baseDir;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getProtocol()
     */
    public String getProtocol()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getId()
     */
    public String getId()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getSnapshots()
     */
    public ArtifactRepositoryPolicy getSnapshots()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getReleases()
     */
    public ArtifactRepositoryPolicy getReleases()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getLayout()
     */
    public ArtifactRepositoryLayout getLayout()
    {
        return null;
    }

    /**
     * @return <code>null</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#getKey()
     */
    public String getKey()
    {
        return null;
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#isUniqueVersion()
     */
    public boolean isUniqueVersion()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.artifact.repository.ArtifactRepository#setBlacklisted(boolean)
     */
    public void setBlacklisted( boolean blackListed )
    {
        // nop
    }

    /**
     * @return <code>false</code>.
     * @see org.apache.maven.artifact.repository.ArtifactRepository#isBlacklisted()
     */
    public boolean isBlacklisted()
    {
        return false;
    }
}
