package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.scanner.MojoScanner;

import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractGeneratorMojo
    extends AbstractPlugin
{
    protected abstract void generate( String outputDirectory, Set mavenMojoDescriptors, MavenProject project )
        throws Exception;

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        MavenProject project = (MavenProject)request.getParameter( "project" );
        
        MojoScanner scanner = (MojoScanner)request.getParameter("mojoScanner");
        
        Set mavenMojoDescriptors = scanner.execute(project);

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        generate( outputDirectory, mavenMojoDescriptors, project );
    }
}
