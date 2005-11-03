package org.apache.maven.plugin.install;

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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Installs a file in local repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal install-file
 * @requiresProject false
 * @aggregator
 */
public class InstallFileMojo
    extends AbstractInstallMojo
{
    /**
     * @parameter expression="${groupId}"
     * @required
     * @readonly
     */
    protected String groupId;

    /**
     * @parameter expression="${artifactId}"
     * @required
     * @readonly
     */
    protected String artifactId;

    /**
     * @parameter expression="${version}"
     * @required
     * @readonly
     */
    protected String version;

    /**
     * @parameter expression="${packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${file}"
     * @required
     * @readonly
     */
    private File file;

    /**
     * @parameter expression="${generatePom}"
     * @readonly
     */
    private boolean generatePom = false;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, packaging );

        // TODO: check if it exists first, and default to true if not
        if ( generatePom )
        {
            FileWriter fw = null;
            try
            {
                File tempFile = File.createTempFile( "mvninstall", ".pom" );
                tempFile.deleteOnExit();

                Model model = new Model();
                model.setModelVersion( "4.0.0" );
                model.setGroupId( groupId );
                model.setArtifactId( artifactId );
                model.setVersion( version );
                model.setPackaging( packaging );
                model.setDescription( "POM was created from install:install-file" );
                fw = new FileWriter( tempFile );
                tempFile.deleteOnExit();
                new MavenXpp3Writer().write( fw, model );
                ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, tempFile );
                artifact.addMetadata( metadata );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error writing temporary pom file: " + e.getMessage(), e );
            }
            finally
            {
                IOUtil.close( fw );
            }
        }

        // TODO: validate
        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        try
        {
            String localPath = localRepository.pathOf( artifact );

            File destination = new File( localRepository.getBasedir(), localPath );

            if( !file.getPath().equals( destination.getPath() ) )
            {
                installer.install( file, artifact, localRepository );
            }
            else
            {
                throw new MojoFailureException( "Cannot install artifact. Artifact is already in the local repository.\n\nFile in question is: " + file + "\n" );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException(
                "Error installing artifact '" + artifact.getDependencyConflictId() + "': " + e.getMessage(), e );
        }
    }
}
