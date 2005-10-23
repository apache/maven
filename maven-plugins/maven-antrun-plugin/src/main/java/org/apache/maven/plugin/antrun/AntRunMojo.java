package org.apache.maven.plugin.antrun;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Target;

/**
 * Maven AntRun Mojo.
 *
 * This plugin provides the capability of calling ant tasks
 * from a POM. It is encouraged to move the actual tasks to
 * a separate build.xml file and call that file with an
 * &lt;ant/&gt; task.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 *
 * @configurator override
 *
 * @goal run
 * 
 * @description Runs the nested ant tasks
 * 
 * @requiresDependencyResolution
 */
public class AntRunMojo
    extends AbstractAntMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${tasks}"
     */
    private Target tasks;

    /**
     * @parameter expression="${sourceRoot}"
     */
    private File sourceRoot;

    /**
     * @parameter expression="${testSourceRoot}"
     */
    private File testSourceRoot;

    /**
     */
    public void execute()
        throws MojoExecutionException
    {
        executeTasks( tasks, project );

        if ( sourceRoot != null )
        {
            getLog().info( "Registering compile source root " + sourceRoot );
            project.addCompileSourceRoot( sourceRoot.toString() );
        }

        if ( testSourceRoot != null )
        {
            getLog().info( "Registering compile test source root " + testSourceRoot );
            project.addTestCompileSourceRoot( testSourceRoot.toString() );
        }

    }
}
