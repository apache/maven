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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Mojo which touches a file without requiring a project.
 * 
 * @goal light-touch
 * @requiresProject false
 * 
 */
public class NoProjectMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /** Test setting of plugin-artifacts on the PluginDescriptor instance.
     * @parameter expression="${plugin.artifactMap}"
     * @required
     */
    private Map pluginArtifacts;

    /**
     * @parameter expression="target/test-basedir-alignment"
     */
    private File basedirAlignmentDirectory;

    /**
     * @parameter
     */
    private String pluginItem = "foo";

    /**
     * @parameter
     */
    private String goalItem = "bar";
    
    /**
     * @parameter expression="${artifactToFile}"
     */
    private String artifactToFile;

    public void execute()
        throws MojoExecutionException
    {
        touch( new File( outputDirectory ), "touch.txt" );

        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File

        if ( basedirAlignmentDirectory.getPath().equals( "target/test-basedir-alignment" ) )
        {
            throw new MojoExecutionException( "basedirAlignmentDirectory not aligned" );
        }
        
        touch( basedirAlignmentDirectory, "touch.txt" );
        
        File outDir = new File( outputDirectory );

        // Test parameter setting
        if ( pluginItem != null )
        {
            touch( outDir, pluginItem );
        }

        if ( goalItem != null )
        {
            touch( outDir, goalItem );
        }
        
        if ( artifactToFile != null )
        {
            Artifact artifact = (Artifact) pluginArtifacts.get( artifactToFile );
            
            File artifactFile = artifact.getFile();
            
            String filename = artifactFile.getAbsolutePath().replace('/', '_').replace(':', '_') + ".txt";
            
            touch( outDir, filename );
        }

        project.getBuild().setFinalName( "coreitified" );
    }

    private void touch( File dir, String file )
        throws MojoExecutionException
    {
        try
        {
             if ( !dir.exists() )
             {
                 dir.mkdirs();
             }
             
             File touch = new File( dir, file );
             
             getLog().info( "Touching: " + touch );
     
             FileWriter w = new FileWriter( touch );
             
             w.write( file );
             
             w.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error touching file", e );
        }
    }
}
