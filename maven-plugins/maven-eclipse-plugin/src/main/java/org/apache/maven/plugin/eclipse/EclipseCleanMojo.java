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

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * A Maven2 plugin to delete the .project, .classpath and .wtpmodules files needed for Eclipse.
 *
 * @goal clean
 */
public class EclipseCleanMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project.basedir}"
     */
    private File basedir;

    public void execute()
        throws MojoExecutionException
    {
        delete( new File( basedir, ".project" ) );
        delete( new File( basedir, ".classpath" ) );
        delete( new File( basedir, ".wtpmodules" ) );
    }

    /**
     * Delete a file, handling log messages and exceptions
     * @param f File to be deleted
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    private void delete( File f )
        throws MojoExecutionException
    {
        getLog().info( MessageFormat.format( "Deleting {0} file...", new Object[] { f.getName() } ) );

        if ( f.exists() )
        {
            if ( !f.delete() )
            {
                throw new MojoExecutionException( MessageFormat.format( "Failed to delete {0} file: {0}", new Object[] {
                    f.getName(),
                    f.getAbsolutePath() } ) )
                {
                };
            }
        }
        else
        {
            getLog().info( MessageFormat.format( "No {0} file found", new Object[] { f.getName() } ) );
        }
    }

}
