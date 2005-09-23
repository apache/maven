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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public abstract class AbstractAntMojo
    extends AbstractMojo
{
    protected void executeTasks( Target antTasks, MavenProject mavenProject )
        throws MojoExecutionException
    {
    	try
        {
            PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(
                antTasks.getProject()
            );

            propertyHelper.setNext(
                new AntPropertyHelper( mavenProject, getLog() )
            );

            DefaultLogger antLogger = new DefaultLogger();
            antLogger.setOutputPrintStream( System.out );
            antLogger.setErrorPrintStream( System.err );
            antLogger.setMessageOutputLevel( Project.MSG_INFO );

            antTasks.getProject().addBuildListener( antLogger );
            antTasks.getProject().setBaseDir( mavenProject.getBasedir() );

            Path p = new Path( antTasks.getProject() );
            p.setPath( StringUtils.join( mavenProject.getCompileClasspathElements().iterator(), File.pathSeparator ) );
            antTasks.getProject().addReference( "maven.dependency.classpath", p );
            antTasks.getProject().addReference( "maven.compile.classpath", p );
            p = new Path( antTasks.getProject() );
            p.setPath( StringUtils.join( mavenProject.getRuntimeClasspathElements().iterator(), File.pathSeparator ) );
            antTasks.getProject().addReference( "maven.runtime.classpath", p );
            p = new Path( antTasks.getProject() );
            p.setPath( StringUtils.join( mavenProject.getTestClasspathElements().iterator(), File.pathSeparator ) );
            antTasks.getProject().addReference( "maven.test.classpath", p );

            getLog().info( "Executing tasks" );

            antTasks.execute();

            getLog().info( "Executed tasks" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing ant tasks", e );
        }
    }
}
