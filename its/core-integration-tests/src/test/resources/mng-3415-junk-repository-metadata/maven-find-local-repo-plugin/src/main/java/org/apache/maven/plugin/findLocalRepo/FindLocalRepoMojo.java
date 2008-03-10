package org.apache.maven.plugin.findLocalRepo;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @goal find
 * @requiresProject false
 */
public class FindLocalRepoMojo
    extends AbstractMojo
{
    /**
     * Location of the file containing the local repository path.
     * @parameter expression="${output}" default-value="${project.build.directory}/local-repository-location.txt"
     * @required
     */
    private File output;

    /**
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {
        if ( !output.exists() )
        {
            output.getParentFile().mkdirs();
        }

        FileWriter w = null;
        try
        {
            w = new FileWriter( output );

            w.write( String.valueOf( new File( localRepository.getBasedir() ).getAbsolutePath() ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file " + output, e );
        }
        finally
        {
            if ( w != null )
            {
                try
                {
                    w.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }
}
