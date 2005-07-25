package org.apache.maven.artifact.transform;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: ArtifactTransformation.java,v 1.1 2005/03/03 15:37:25
 *          jvanzyl Exp $
 */
public interface ArtifactTransformation
{
    String ROLE = ArtifactTransformation.class.getName();

    /**
     * Take in a artifact and return the transformed artifact for locating in the remote repository. If no
     * transformation has occured the original artifact is returned.
     *
     * @param artifact Artifact to be transformed.
     * @param remoteRepositories the repositories to check
     * @param localRepository the local repository
     */
    void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    /**
     * Take in a artifact and return the transformed artifact for locating in the local repository. If no
     * transformation has occured the original artifact is returned.
     *
     * @param artifact Artifact to be transformed.
     * @param localRepository the local repository it will be stored in
     */
    void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    /**
     * Take in a artifact and return the transformed artifact for distributing toa remote repository. If no
     * transformation has occured the original artifact is returned.
     *
     * @param artifact Artifact to be transformed.
     * @param remoteRepository the repository to deploy to
     */
    void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException;
}