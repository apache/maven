package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractGeneratorMojo
    extends AbstractPlugin
{
    protected abstract void generate( String sourceDirectory, String outputDirectory, String pom )
        throws Exception;

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String sourceDirectory = (String) request.getParameter( "sourceDirectory" );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String pom = (String) request.getParameter( "pom" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        generate( sourceDirectory, outputDirectory, pom );
    }
}
