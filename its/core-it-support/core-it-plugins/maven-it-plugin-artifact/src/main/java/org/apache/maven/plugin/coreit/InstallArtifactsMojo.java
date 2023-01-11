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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Mojo( name = "install-artifacts", requiresDependencyResolution = ResolutionScope.RUNTIME,
       defaultPhase = LifecyclePhase.PACKAGE )
public class InstallArtifactsMojo
    extends AbstractMojo
{

    /**
     */
    @Parameter( defaultValue = "${project.runtimeArtifacts}", readonly = true )
    private List<Artifact> artifacts;

    /**
     */
    @Component
    private ArtifactInstaller artifactInstaller;

    /**
     */
    @Component
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * The directory that will be used to assemble the artifacts in
     * and place the bin scripts.
     */
    @Parameter( property = "assembleDirectory", defaultValue = "${project.build.directory}/appassembler" )
    private File assembleDirectory;

    /**
     * Path (relative to assembleDirectory) of the desired output repository.
     */
    @Parameter( defaultValue = "repo" )
    private String repositoryName;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        ArtifactRepositoryLayout artifactRepositoryLayout = new FlatRepositoryLayout();

        // The repo where the jar files will be installed
        ArtifactRepository artifactRepository =
            artifactRepositoryFactory.createDeploymentArtifactRepository( "appassembler", "file://"
                + assembleDirectory.getAbsolutePath() + "/" + repositoryName, artifactRepositoryLayout, false );
        for ( Artifact artifact : artifacts )
        {
            installArtifact( artifactRepository, artifact );
        }
    }

    private void installArtifact( ArtifactRepository artifactRepository, Artifact artifact )
        throws MojoExecutionException
    {
        try
        {

            artifact.isSnapshot();

            if ( artifact.getFile() != null )
            {
                artifactInstaller.install( artifact.getFile(), artifact, artifactRepository );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "Failed to copy artifact.", e );
        }
    }

    /**
     *
     */
    public static class FlatRepositoryLayout
        implements ArtifactRepositoryLayout
    {
        private static final char ARTIFACT_SEPARATOR = '-';

        private static final char GROUP_SEPARATOR = '.';

        @Override
        public String getId()
        {
            return "id";
        }

        public String pathOf( Artifact artifact )
        {
            ArtifactHandler artifactHandler = artifact.getArtifactHandler();

            StringBuilder path = new StringBuilder();

            path.append( artifact.getArtifactId() ).append( ARTIFACT_SEPARATOR ).append( artifact.getVersion() );

            if ( artifact.hasClassifier() )
            {
                path.append( ARTIFACT_SEPARATOR ).append( artifact.getClassifier() );
            }

            if ( artifactHandler.getExtension() != null && artifactHandler.getExtension().length() > 0 )
            {
                path.append( GROUP_SEPARATOR ).append( artifactHandler.getExtension() );
            }

            return path.toString();
        }

        public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
        {
            return pathOfRepositoryMetadata( metadata.getLocalFilename( repository ) );
        }

        private String pathOfRepositoryMetadata( String filename )
        {
            StringBuilder path = new StringBuilder();

            path.append( filename );

            return path.toString();
        }

        public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
        {
            return pathOfRepositoryMetadata( metadata.getRemoteFilename() );
        }
    }

}
