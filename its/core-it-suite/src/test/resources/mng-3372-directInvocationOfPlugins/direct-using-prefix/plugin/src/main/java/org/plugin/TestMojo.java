package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @goal test
 */
public class TestMojo
    implements Mojo
{

    private Log log;

    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File buildDir;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File outFile = new File( buildDir, "out.txt" );
        FileWriter writer = null;
        try
        {
            outFile.getParentFile().mkdirs();

            writer = new FileWriter( outFile );
            writer.write( "Test" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write: " + outFile.getAbsolutePath(), e );
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
                }
            }
        }
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }
}
