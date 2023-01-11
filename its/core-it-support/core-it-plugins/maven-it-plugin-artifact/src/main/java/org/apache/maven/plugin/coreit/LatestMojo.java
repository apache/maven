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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Marks the project's artifact as the latest version.
 *
  *
 * @author Benjamin Bentmann
 */
@Mojo( name = "latest", defaultPhase = LifecyclePhase.PACKAGE )
public class LatestMojo
    extends AbstractMojo
{

    /**
     * The main project artifact.
     */
    @Parameter( defaultValue = "${project.artifact}", readonly = true, required = true )
    private Artifact projectArtifact;

    /**
     * Runs this mojo.
     */
    public void execute()
        throws MojoExecutionException
    {
        Versioning versioning = new Versioning();
        versioning.setLatest( projectArtifact.getVersion() );
        versioning.updateTimestamp();

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( projectArtifact, versioning );
        projectArtifact.addMetadata( metadata );

        getLog().info( "[MAVEN-CORE-IT-LOG] Marked artifact as latest" );
    }

}
