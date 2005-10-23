package org.apache.maven.plugin.assembly;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;
import java.util.Iterator;

/**
 * Unpack project dependencies.  Currently supports dependencies of type jar and zip.
 *
 * @version $Id$
 * @goal unpack
 * @requiresDependencyResolution test
 */
public class UnpackMojo
    extends AbstractUnpackingMojo
{
    /**
     * Unpacks the archive file.
     *
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        for ( Iterator j = getDependencies().iterator(); j.hasNext(); )
        {
            Artifact artifact = (Artifact) j.next();

            String name = artifact.getFile().getName();

            File tempLocation = new File( workDirectory, name.substring( 0, name.length() - 4 ) );
            boolean process = false;
            if ( !tempLocation.exists() )
            {
                tempLocation.mkdirs();
                process = true;
            }
            else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
            {
                process = true;
            }

            if ( process )
            {
                File file = artifact.getFile();
                try
                {
                    unpack( file, tempLocation );
                }
                catch ( NoSuchArchiverException e )
                {
                    this.getLog().info( "Skip unpacking dependency file with unknown extension: " + file.getPath() );
                }
            }
        }
    }


}