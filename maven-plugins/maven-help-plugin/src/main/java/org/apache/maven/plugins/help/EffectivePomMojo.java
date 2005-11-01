package org.apache.maven.plugins.help;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

/** Display the effective POM for this build, with the active profiles factored in.
 * 
 * @goal effective-pom
 * @aggregator
 */
public class EffectivePomMojo
    extends AbstractMojo
{

    /**
     * The projects in the current build. The effective-POM for
     * each of these projects will written.
     * 
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List projects;

    /**
     * If specified, write the output to this path.
     * 
     * @parameter expression="${output}"
     */
    private File output;

    public void execute()
        throws MojoExecutionException
    {
        StringBuffer message = new StringBuffer();
        
        for ( Iterator it = projects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            
            getEffectivePom( project, message );
            
            message.append( "\n\n" );
        }
        
        if ( output != null )
        {
            FileWriter fWriter = null;
            try
            {
                File dir = output.getParentFile();

                if ( !dir.exists() )
                {
                    dir.mkdirs();
                }

                getLog().info( "Writing effective-POM to: " + output );

                fWriter = new FileWriter( output );

                fWriter.write( "Created by: " + getClass().getName() + "\n" );
                fWriter.write( "Created on: " + new Date() + "\n\n" );
                
                fWriter.write( message.toString() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write effective-POM to output: " + output, e );
            }
            finally
            {
                if ( fWriter != null )
                {
                    try
                    {
                        fWriter.close();
                    }
                    catch ( IOException e )
                    {
                        getLog().debug( "Cannot close FileWriter to output location: " + output, e );
                    }
                }
            }
        }
        else
        {
            StringBuffer formatted = new StringBuffer();

            formatted.append( "\nEffective POMs, after inheritance, interpolation, and profiles are applied:\n\n" );
            formatted.append( message.toString() );
            formatted.append( "\n" );

            getLog().info( message );
        }
    }

    private void getEffectivePom( MavenProject project, StringBuffer message ) 
        throws MojoExecutionException
    {
        Model pom = project.getModel();

        StringWriter sWriter = new StringWriter();

        MavenXpp3Writer pomWriter = new MavenXpp3Writer();

        try
        {
            pomWriter.write( sWriter, pom );
            
            message.append( "\n************************************************************************************" );
            message.append( "\nEffective POM for project \'" + project.getId() + "\'" );
            message.append( "\n************************************************************************************" );
            message.append( "\n" );
            message.append( sWriter.toString() );
            message.append( "\n************************************************************************************" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot serialize POM to XML.", e );
        }

    }

    protected final void setOutput( File output )
    {
        this.output = output;
    }

    protected final void setProjects( List projects )
    {
        this.projects = projects;
    }

}
