package org.apache.maven.plugins.it0089;

import com.puppycrawl.tools.checkstyle.PackageNamesLoader;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;


/**
 * @goal test
 */
public class Mojo
    extends AbstractMojo
{
    
    /**
     * @parameter default-value="${basedir}/target"
     * @required
     * @readonly
     */
    private File outDir;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            PackageNamesLoader.loadModuleFactory(Thread.currentThread().getContextClassLoader());
            getLog().info( "Loaded checkstyle module factory.");
            
            outDir.mkdirs();
            
            File output = new File( outDir, "output.txt" );
            Writer writer = null;
            
            try
            {
                writer = new FileWriter( output );
                writer.write( "Success." );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to write output file.", e );
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
        catch ( CheckstyleException e )
        {
            throw new MojoExecutionException( "Error loading checkstyle module factory.", e );
        }
    }

}
