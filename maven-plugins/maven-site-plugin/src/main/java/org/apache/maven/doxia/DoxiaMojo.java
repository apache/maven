package org.apache.maven.doxia;

import org.apache.maven.plugin.Plugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.doxia.site.renderer.SiteRenderer;

/**
 * @goal site
 * @description Doxia plugin
 * @parameter name="siteDirectory"
 * type=""
 * required=""
 * validator=""
 * expression="#basedir/src/site"
 * description=""
 * @parameter name="generatedSiteDirectory"
 * type=""
 * required=""
 * validator=""
 * expression="#project.build.directory/site-generated"
 * description=""
 * @parameter name="outputDirectory"
 * type=""
 * required=""
 * validator=""
 * expression="#project.build.directory/site"
 * description=""
 * @parameter name="project"
 * type=""
 * required=""
 * validator=""
 * expression="#project"
 * description=""
 * @parameter name="siteRenderer"
 * type=""
 * required=""
 * validator=""
 * expression="#component.org.codehaus.doxia.site.renderer.SiteRenderer"
 * description=""
 * @parameterx name="reportManager"
 * type=""
 * required=""
 * validator=""
 * expression="#component.org.apache.maven.reporting.MavenReportManager"
 * description=""
 */
public class DoxiaMojo
    extends AbstractPlugin
{
    private String siteDirectory;

    private String generatedSiteDirectory;

    private String outputDirectory;

    private  SiteRenderer siteRenderer;

    private MavenProject project;

    public void execute()
        throws PluginExecutionException
    {
        try
        {
            /*
            for ( Iterator i = project.getReports().iterator(); i.hasNext(); )
            {
                String name = (String) i.next();

                reportManager.executeReport( name, project.getModel(), siteRenderer, outputDirectory );
            }
            */

            siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory );
        }
        catch ( Exception e )
        {
            // TODO: handle it
            e.printStackTrace();
        }
    }
}
