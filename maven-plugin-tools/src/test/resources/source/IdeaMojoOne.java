package org.apache.maven.plugin.idea;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

/**
 * @goal ideaOne
 *
 * @description Create an IDEA project file from a Maven project.
 *
 * @requiresDependencyResolution
 *
 * @prereq foo
 * @prereq bar
 *
 * @parameter
 *   name="project"
 *   type="String[]"
 *   required="true"
 *   validator="org.foo.validator"
 *   expression="#project"
 *   description="Maven project used to generate IDEA project files."
 */
public class IdeaMojoOne
    extends AbstractPlugin
{
    protected IdeaWriter ideaWriter;

    public IdeaPlugin()
    {
        ideaWriter = new IdeaWriter();
    }

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        MavenProject project = (MavenProject) request.getParameter( "project" );

        ideaWriter.write( project );
    }
}
