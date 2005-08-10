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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.PropertyHelper;
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
 */
public class AntRunMojo
    extends AbstractMojo
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
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(
                tasks.getProject()
            );

            propertyHelper.setNext(
                new AntPropertyHelper( project, getLog() )
            );

            DefaultLogger antLogger = new DefaultLogger();
            antLogger.setOutputPrintStream( System.out );
            antLogger.setErrorPrintStream( System.err );
            tasks.getProject().addBuildListener( antLogger );

            getLog().info( "Executing tasks" );

            tasks.execute();

            getLog().info( "Executed tasks" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing ant tasks", e );
        }
    }
}
