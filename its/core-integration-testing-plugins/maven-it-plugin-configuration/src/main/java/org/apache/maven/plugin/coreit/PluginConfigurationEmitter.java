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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Properties;

//MAPI: This is a canidate for the internal state dump (ISD). This is probably similar to what is in the help plugin.

// Compare:
//   MavenProject
//   PluginExpressionEvaluator
//   Raw DOM
//
// Currently we don't have the means to easily test this inside the core. So this will be a model to drive
// making the core more testable.

/**
 * @goal config
 * @phase generate-resources
 * @description Goal produces a raw string with contains full interpolated plugin configurations.
 */
public class PluginConfigurationEmitter
    extends AbstractMojo
{
    /**
     * The MavenProject we will use for comparision.
     *
     * @parameter expression="${project}"
     */
    private MavenProject project;

    // How to enumerate all the possible expressions that can be used.

    /**
     * This is the raw interpolated DOM will be used for comparison.
     *
     * @parameter expression="${dom}"
     */
    private PlexusConfiguration dom;

    /** @parameter expression="${directory}" default-value="${project.build.directory}" */
    private File directory;

    /**
     * Where to place the serialized version of the DOM for analysis.
     *
     * @parameter expression="${fileName}" default-value="interpolated-plugin-configuration.xml"
     */
    private String fileName;

    public void execute()
        throws MojoExecutionException
    {
        if ( !directory.exists() )
        {
            directory.mkdirs();
        }

        emitMavenProjectValues();

        emitExpressionEvaluatorValues();

        emitRawDomValues();
    }

    private void emitMavenProjectValues()
        throws MojoExecutionException
    {
        try
        {
            Properties p = new Properties();

            p.setProperty( "project.build.directory", directory.getAbsolutePath() );

            File file = new File( directory, "maven-project-output.txt" );

            OutputStream os = new FileOutputStream( file );

            p.store( os, "expression evaluator values" );

            os.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing out plugin configuration.", e );
        }
    }

    private void emitExpressionEvaluatorValues()
        throws MojoExecutionException
    {
        try
        {
            Properties p = new Properties();

            p.setProperty( "project.build.directory", directory.getAbsolutePath() );

            File file = new File( directory, "expression-evaluator-output.txt" );

            OutputStream os = new FileOutputStream( file );

            p.store( os, "expression evaluator values" );

            os.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing out plugin configuration.", e );
        }
    }

    private void emitRawDomValues()
        throws MojoExecutionException
    {
        try
        {
            File file = new File( directory, fileName );

            Writer writer = new FileWriter( file );

            writer.write( dom.toString() );

            writer.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing out plugin configuration.", e );
        }
    }
}
