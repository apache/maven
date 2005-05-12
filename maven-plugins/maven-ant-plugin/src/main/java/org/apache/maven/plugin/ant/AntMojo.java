package org.apache.maven.plugin.ant;

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

/**
 * A Maven2 plugin to generate an Ant build file.
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal ant
 * @requiresDependencyResolution
 * @todo change this to use the artifact ant tasks instead of :get
 */
public class AntMojo
    extends AbstractMojo
{
    /**
     * The project to create a build for.
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The location of the local repository.
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {
        // TODO: read back previous

        AntBuildWriter antBuildWriter = new AntBuildWriter( project, new File( localRepository.getBasedir() ) );

        try
        {
            antBuildWriter.write();
        }
        catch ( AntPluginException e )
        {
            throw new MojoExecutionException( "Error building Ant script", e );
        }
    }
}
