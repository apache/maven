package $package;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.io.FileWriter;

/**
 * @goal touch
 * 
 * @phase process-sources
 *
 * @description Goal which cleans the build
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 * 
 * @parameter
 *  name="basedirAlignmentDirectory"
 *  type="java.io.File"
 *  required="true"
 *  validator=""
 *  expression="target/test-basedir-alignment"
 *  description=""
 */
public class MyMojo
    extends AbstractPlugin
{
    private static final int DELETE_RETRY_SLEEP_MILLIS = 10;

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        File f = new File( outputDirectory );
        
        if ( !f.exists() )
        {
            f.mkdirs();
        }
        
        File touch = new File( f, "touch.txt" );
        
        FileWriter w = new FileWriter( touch );
        
        w.write( "touch.txt" );
        
        w.close();
        
        // This parameter should be aligned to the basedir as the parameter type is specified
        // as java.io.File
        
        String basedirAlignmentDirectory = (String) request.getParameter( "basedirAlignmentDirectory" );

        f = new File( basedirAlignmentDirectory );
        
        if ( !f.exists() )
        {
            f.mkdirs();
        }         
        
        touch = new File( f, "touch.txt" );
        
        w = new FileWriter( touch );
        
        w.write( "touch.txt" );
        
        w.close();        
    }
}
