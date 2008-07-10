package org.apache.maven.plugin.coreit;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal check-property
 * 
 * @phase validate
 */
public class PropertyInterpolationMojo
    extends AbstractMojo
{
    
    private static final char FS = File.separatorChar;
    
    /** @parameter */
    private String myDirectory;

    /** @parameter expression="${project}" */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        String value = project.getProperties().getProperty( "myDirectory" ).replace( '/', FS ).replace( '\\', FS );
        
        String targetValue = project.getBuild().getDirectory() + FS + "foo";
        targetValue = targetValue.replace( '/', FS).replace( '\\', FS );
        
        if ( !value.equals( targetValue ) )
        {
            throw new MojoExecutionException( "Property value of 'myDirectory': " + value + " should equal project build directory: " + project.getBuild().getDirectory() + " + '/foo'" );
        }
    }
}
