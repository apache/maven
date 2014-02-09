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
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @goal touch
 *
 * @phase process-sources
 *
 * @description Goal which cleans the build
 */
public class CoreItMojo
    extends AbstractMojo
{
    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /** Test setting of plugin-artifacts on the PluginDescriptor instance.
     * @parameter default-value="${plugin.artifactMap}"
     * @required
     */
    private Map pluginArtifacts;

    /**
     * @parameter default-value="target/test-basedir-alignment"
     */
    private File basedirAlignmentDirectory;

    /**
     * @parameter alias="pluginFile"
     */
    private String pluginItem = "foo";

    /**
     * @parameter
     */
    private String goalItem = "bar";

    /**
     * @parameter property="artifactToFile"
     */
    private String artifactToFile;

    /**
     * @parameter property="fail"
     */
    private boolean fail = false;

    public void execute()
        throws MojoExecutionException
    {
        if ( fail )
        {
            throw new MojoExecutionException( "Failing per \'fail\' parameter (specified in pom or system properties)" );
        }

        touch( new File( outputDirectory ), "touch.txt" );

        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File

        if ( !basedirAlignmentDirectory.isAbsolute() )
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
