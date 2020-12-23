package org.apache.maven.plugin.coreit;

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

import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Installs the project artifacts into a local repository with a custom base directory and a custom layout.
 *
 * @goal install-custom
 *
 * @author Benjamin Bentmann
 *
 */
public class InstallCustomMojo
    extends InstallMojo
{

    /**
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * The base directory of the local repository to install to.
     *
     * @parameter property="install.localRepoDir"
     */
    private File localRepoDir;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If any artifact could not be installed.
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            String url = "file://" + localRepoDir.toURL().getPath();

            localRepository =
                repositoryFactory.createArtifactRepository( localRepository.getId(), url, new CustomRepositoryLayout(),
                                                            localRepository.getSnapshots(),
                                                            localRepository.getReleases() );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to create custom local repository", e );
        }

        super.execute();
    }

}
