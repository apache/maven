package jar;

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
 * @goal test
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     * @parameter default-value="${project.build.directory}"
     * @readonly
     */
    private File buildDirectory;
    
    /**
     * @parameter
     * @required
     */
    private String config;

    public void execute()
        throws MojoExecutionException
    {
        if ( !buildDirectory.isAbsolute() )
        {
            throw new MojoExecutionException( "The expression 'project.build.directory' didn't render an absolute path when injected from a plugin parameter default value." );
        }
        
        if ( config.indexOf( buildDirectory.getAbsolutePath() ) < 0 )
        {
            throw new MojoExecutionException( "The expression 'project.build.directory' didn't render an absolute path when used inside a plugin configuration of type String." );
        }
    }
}
