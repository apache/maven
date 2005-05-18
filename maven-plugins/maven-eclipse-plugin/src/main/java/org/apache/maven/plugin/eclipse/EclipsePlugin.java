package org.apache.maven.plugin.eclipse;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * A Maven2 plugin which integrates the use of Maven2 with Eclipse.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 * @goal eclipse
 * @requiresDependencyResolution
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
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

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

        try
        {
            eclipseWriter.setLocalRepositoryFile( new File ( localRepository.getBasedir() ) );

            eclipseWriter.write( project );
        }
        catch ( EclipsePluginException e )
        {
            throw new MojoExecutionException( "Error writing eclipse files.", e );
        }
    }
}
