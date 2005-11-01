package org.apache.maven.plugins.help;

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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

/** Lists the profiles which are currently active for this build.
 * 
 * @goal active-profiles
 * @aggregator
 */
public class ActiveProfilesMojo extends AbstractMojo
{
    
    /**
     * This is the list of projects currently slated to be built by Maven.
     * 
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List projects;
    
    /**
     * This is an optional parameter for a file destination for the output
     * of this mojo...the listing of active profiles per project.
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
            
            getActiveProfileStatement( project, message );
            
            message.append( "\n\n" );
        }
        
        if ( output != null )
        {
            writeFile( message );
        }
        else
        {
            Log log = getLog();
            log.info( message );
        }
    }
    
    private void writeFile( StringBuffer message ) 
        throws MojoExecutionException
    {
        Writer writer = null;
        try
        {
            File dir = output.getParentFile();
            
            if( !dir.exists() )
            {
                dir.mkdirs();
            }
            
            writer = new FileWriter( output );
            
            writer.write( "Created by: " + getClass().getName() + "\n" );
            writer.write( "Created on: " + new Date() + "\n\n" );
            writer.write( message.toString() );
            writer.flush();
            
            getLog().info( "Active profile report written to: " + output );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write output to file: " + output, e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    getLog().debug( "Failed to close output file writer.", e );
                }
            }
        }
    }

    private void getActiveProfileStatement( MavenProject project, StringBuffer message )
    {
        List profiles = project.getActiveProfiles();
        
        message.append( "\n" );
        
        message.append( "Active Profiles for Project \'" + project.getId() + "\': \n\n" );
        
        if( profiles == null || profiles.isEmpty() )
        {
            message.append( "There are no active profiles." );
        }
        else
        {
            message.append( "The following profiles are active:\n" );
            
            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();
                
                message.append( "\n - " )
                       .append( profile.getId() )
                       .append(" (source: " )
                       .append( profile.getSource() ).append( ")" );
            }
            
        }
        
        message.append( "\n" );
    }

    public final void setProjects( List projects )
    {
        this.projects = projects;
    }

}
