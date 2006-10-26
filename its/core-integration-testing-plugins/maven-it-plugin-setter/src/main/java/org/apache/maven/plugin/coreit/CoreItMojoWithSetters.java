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
     *   property="outputDirectory"
     *   expression="${project.build.directory}"
     * @required
     */
    private String outputDirectoryValue;

    /**
     * @parameter property="foo"
     */
    private String fooValue;

    /**
     * @parameter
     */
    private String bar;

    // ----------------------------------------------------------------------
    // Setters
    // ----------------------------------------------------------------------

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectoryValue = outputDirectory;
    }

    boolean setFooSetterExecuted;

    public void setFoo( String fooValue )
    {
        this.fooValue = fooValue;

        setFooSetterExecuted = true;
    }

    boolean setBarSetterExecuted;

    public void setBar( String barValue )
    {
        this.bar = barValue + ".baz";

        setBarSetterExecuted = true;
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
        if ( fooValue != null && setFooSetterExecuted )
        {
            touch( outDir, fooValue );
        }

        if ( bar != null && setBarSetterExecuted )
        {
            touch( outDir, bar );
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
