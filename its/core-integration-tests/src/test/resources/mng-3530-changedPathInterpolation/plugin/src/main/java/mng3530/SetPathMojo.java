package mng3530;

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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Modify the build path project.build.directory, contained in the current
 * project instance. This and the corresponding {@link ValidatePathMojo} should
 * prove or disprove the ability to have build-path modifications ripple through
 * the project's values, such as plugin configurations.
 *
 * @goal set
 * @phase compile
 */
public class SetPathMojo
    implements Mojo
{
    public static final String MODIFIED_BUILD_DIRECTORY_NAME = "target-modified";

    /**
     * Project instance to modify.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private Log log;

    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "Before modification, project.build.directory is: " + project.getBuild().getDirectory() );

        File basedir = project.getBasedir();
        project.getBuild().setDirectory( new File( basedir, MODIFIED_BUILD_DIRECTORY_NAME ).getAbsolutePath() );

        getLog().info( "After modification, project.build.directory is: " + project.getBuild().getDirectory() );
        getLog().info( "Modifications complete." );
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }
}
