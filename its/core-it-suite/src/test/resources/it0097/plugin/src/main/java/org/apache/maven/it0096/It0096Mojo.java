package org.apache.maven.it0096;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal it0097
 */
public class It0096Mojo extends AbstractMojo
{
    
    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File targetDirectory;

    public void execute() throws MojoExecutionException
    {
        targetDirectory.mkdirs();

        File myFile = new File( targetDirectory, "it0097.txt" );
        
        if ( myFile.exists() )
        {
            throw new MojoExecutionException( "This mojo has already been run, or the project wasn't cleaned." );
        }
        else
        {
            FileWriter writer = null;
            try
            {
                writer = new FileWriter( myFile );
                writer.write( "test" );
                writer.close();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to write test file: " + myFile );
            }
            finally
            {
                if ( writer != null )
                {
                    try{ writer.close(); }
                    catch( IOException e ) {}
                }
            }            
        }
    }
}
