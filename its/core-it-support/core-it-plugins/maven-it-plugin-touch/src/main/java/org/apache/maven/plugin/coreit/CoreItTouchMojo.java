package org.apache.maven.plugin.coreit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Mojo that creates one <code>touch.txt</code> or more files with configured filenames in <code>target/</code>
 * directory, or cause failure if desired, and set build final name to '<code>coreitified</code>'
 *
  */
@Mojo( name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class CoreItTouchMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${project}" )
    private MavenProject project;

    /**
     * Output directory for touched files.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private String outputDirectory;

    /**
     * Test setting of plugin-artifacts on the PluginDescriptor instance.
     */
    @Parameter( defaultValue = "${plugin.artifactMap}", required = true )
    private Map<String, Artifact> pluginArtifacts;

    /**
     * Parameter to check that File attribute is injected with absolute path, even if parameter
     * value is relative: a <code>touch.txt</code> file will be created in specified directory, to be able
     * to check that absolute value is at right place.
     */
    @Parameter( defaultValue = "target/test-basedir-alignment" )
    private File basedirAlignmentDirectory;

    /**
     */
    @Parameter( alias = "pluginFile" )
    private String pluginItem = "foo";

    /**
     */
    @Parameter
    private String goalItem = "bar";

    /**
     * Touch a file named after artifact absolute file name, replacing '/' and ':' by '_' and adding ".txt".
     */
    @Parameter( property = "artifactToFile" )
    private String artifactToFile;

    /**
     * Should the goal cause a failure before doing anything else?
     */
    @Parameter( property = "fail" )
    private boolean fail = false;

    public void execute()
        throws MojoExecutionException
    {
        if ( fail )
        {
            throw new MojoExecutionException( "Failing per \'fail\' parameter"
                + " (specified in pom or system properties)" );
        }

        File outDir = new File( outputDirectory );

        touch( outDir, "touch.txt" );

        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File
        if ( !basedirAlignmentDirectory.isAbsolute() )
        {
            throw new MojoExecutionException( "basedirAlignmentDirectory not aligned" );
        }
        touch( basedirAlignmentDirectory, "touch.txt" );

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
            Artifact artifact = pluginArtifacts.get( artifactToFile );

            File artifactFile = artifact.getFile();

            String filename = artifactFile.getAbsolutePath().replace( '/', '_' ).replace( ':', '_' ) + ".txt";

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

             getLog().info( "Touching file: " + touch.getAbsolutePath() );

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
