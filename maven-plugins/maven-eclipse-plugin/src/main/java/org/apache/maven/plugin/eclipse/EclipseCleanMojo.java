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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * A Maven2 plugin to delete the .project and .classpath files needed for Eclipse
 *
 * @goal clean
 */
public class EclipseCleanMojo extends AbstractMojo
{
    /**
     * @parameter expression="${project.basedir}"
     */
    private String basedir;
    
    public void execute() throws MojoExecutionException
    {
        File f = new File( basedir, ".project" );
        
        getLog().info( "Deleting project file..." );
        if ( f.exists() )
        {
            if ( !f.delete() )
            {
                throw new MojoExecutionException( "Failed to delete project file: " + f.getAbsolutePath() );
            }
        }
        else
            getLog().info( "No .project file found." );
        
        f = new File( basedir, ".classpath" );
        
        getLog().info( "Deleting classpath file..." );
        if ( f.exists() )
        {
            if ( !f.delete() )
            {
                throw new MojoExecutionException( "Failed to delete classpath file: " + f.getAbsolutePath() );
            }
        }
        else
            getLog().info( "No .classpath file found." );
    }
}
