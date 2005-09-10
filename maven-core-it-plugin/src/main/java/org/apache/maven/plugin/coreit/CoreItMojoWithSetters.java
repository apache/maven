/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @goal setter-touch
 *
 * @description Goal which cleans the build
 */
public class CoreItMojoWithSetters
    extends AbstractMojo
{
    /**
     * @parameter
     *   property="outputDirectoryValue"
     *   expression="${project.build.directory}"
     * @required
     */
    private String outputDirectoryValue;

    /**
     * @parameter property="foo"
     */
    private String fooValue;

    /**
     * @parameter property="bar"
     */
    private String barValue;

    // ----------------------------------------------------------------------
    // Setters
    // ----------------------------------------------------------------------

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectoryValue = outputDirectory;
    }

    public void setFoo( String fooValue )
    {
        this.fooValue = fooValue;
    }

    public void setBar( String barValue )
    {
        this.barValue = barValue;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------


    public void execute()
        throws MojoExecutionException
    {
        touch( new File( outputDirectoryValue ), "touch.txt" );

        File outDir = new File( outputDirectoryValue );

        // Test parameter setting
        if ( fooValue != null )
        {
            touch( outDir, fooValue );
        }

        if ( barValue != null )
        {
            touch( outDir, barValue );
        }
    }

    private static void touch( File dir, String file )
        throws MojoExecutionException
    {
        try
        {
             if ( !dir.exists() )
             {
                 dir.mkdirs();
             }

             File touch = new File( dir, file );

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
