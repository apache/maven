package org.apache.maven.plugins.mavenone;

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
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Install the artifact in a maven one local repository
 *
 * @goal install-maven-one-repository
 * @phase install
 */
public class MavenOneRepositoryInstallMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * @component
     */
    protected ArtifactRepositoryFactory factory;

    /**
     * @parameter expression="${mavenOneRepository}"
     */
    protected String mavenOneRepository;

    /**
     * @component roleHint="legacy"
     */
    private ArtifactRepositoryLayout legacyLayout;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            if ( mavenOneRepository == null )
            {
                File f = new File( System.getProperty( "user.home" ), "build.properties" );
                if ( f.exists() )
                {
                    Properties p = new Properties();
                    FileInputStream inStream = new FileInputStream( f );
                    try
                    {
                        p.load( inStream );
                    }
                    finally
                    {
                        IOUtil.close( inStream );
                    }
                    mavenOneRepository = p.getProperty( "maven.repo.local" );
                }

                if ( mavenOneRepository == null )
                {
                    mavenOneRepository = System.getProperty( "user.home" ) + "/.maven/repository";
                }
            }

            File f = new File( mavenOneRepository );
            if ( !f.exists() )
            {
                f.mkdirs();
            }

            ArtifactRepository localRepository = factory.createDeploymentArtifactRepository( "mavenOneRepository",
                                                                                             f.toURL().toString(),
                                                                                             legacyLayout, false );

            boolean isPomArtifact = "pom".equals( packaging );

            if ( isPomArtifact )
            {
                installer.install( pomFile, artifact, localRepository );
            }
            else
            {
                File file = artifact.getFile();
                if ( file == null )
                {
                    throw new MojoExecutionException(
                        "The packaging for this project did not assign a file to the build artifact" );
                }
                installer.install( file, artifact, localRepository );
            }

        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
