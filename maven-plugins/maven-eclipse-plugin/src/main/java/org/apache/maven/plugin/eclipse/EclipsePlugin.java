package org.apache.maven.plugin.eclipse;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * A Maven2 plugin which integrates the use of Maven2 with Eclipse.
 *
 * @goal eclipse
 * @requiresDependencyResolution test
 * @execute phase="generate-sources"
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EclipsePlugin
    extends AbstractMojo
{
    protected EclipseWriter eclipseWriter;

    /**
     * The project whose project files to create.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @parameter expression="${eclipse.workspace}"
     */
    private File outputDir;

    public EclipsePlugin()
    {
        eclipseWriter = new EclipseWriter();
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public void execute()
        throws MojoExecutionException
    {
        if ( project.getFile() == null || !project.getFile().exists() )
        {
            throw new MojoExecutionException( "There must be a POM in the current working directory for the Eclipse plugin to work." );
        }

        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Don't generate Eclipse project for pom project" );

            return;
        }

        if ( outputDir == null )
        {
            outputDir = project.getFile().getParentFile();
        }
        else
        {
            if ( !outputDir.isDirectory() )
            {
                throw new MojoExecutionException( "Not a directory: '" + outputDir + "'" );
            }

            outputDir = new File( outputDir, project.getArtifactId() );

            if ( !outputDir.isDirectory() && !outputDir.mkdir() )
            {
                throw new MojoExecutionException( "Can't create directory '" + outputDir + "'" );
            }
        }

        try
        {
            eclipseWriter.setLocalRepositoryFile( new File ( localRepository.getBasedir() ) );

            eclipseWriter.setLog( getLog() );

            if ( executedProject == null )
            {
                // backwards compat with alpha-2 only
                executedProject = project;
            }

            eclipseWriter.write( outputDir, project, executedProject, reactorProjects );
        }
        catch ( EclipsePluginException e )
        {
            throw new MojoExecutionException( "Error writing eclipse files.", e );
        }
    }
}
