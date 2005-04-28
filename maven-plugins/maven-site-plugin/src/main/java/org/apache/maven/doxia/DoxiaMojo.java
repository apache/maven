package org.apache.maven.doxia;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.Plugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.apache.maven.reporting.manager.MavenReportManager;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.util.Iterator;
import java.util.List;

/**
 * @goal site
 * @description Doxia plugin
 * @parameter name="siteDirectory"
 * type=""
 * required=""
 * validator=""
 * expression="${basedir}/src/site"
 * description=""
 * @parameter name="generatedSiteDirectory"
 * type=""
 * required=""
 * validator=""
 * expression="${project.build.directory}/site-generated"
 * description=""
 * @parameter name="outputDirectory"
 * type=""
 * required=""
 * validator=""
 * expression="${project.build.directory}/site"
 * description=""
 * @parameter name="flavour"
 * type="String"
 * required=""
 * validator=""
 * expression="maven"
 * description=""
 * @parameter name="project"
 * type=""
 * required=""
 * validator=""
 * expression="${project}"
 * description=""
 * @parameter name="localRepository"
 * type="org.apache.maven.artifact.ArtifactRepository"
 * required="true"
 * validator=""
 * expression="#localRepository"
 * description=""
 * @parameter name="remoteRepositories"
 * type="java.util.List"
 * required="true"
 * validator=""
 * expression="#project.remoteArtifactRepositories"
 * description=""
 * @parameter name="siteRenderer"
 * type=""
 * required=""
 * validator=""
 * expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
 * description=""
 * @parameter name="reportManager"
 * type=""
 * required=""
 * validator=""
 * expression="${component.org.apache.maven.reporting.manager.MavenReportManager}"
 * description=""
 */
public class DoxiaMojo
    extends AbstractPlugin
{
    private String siteDirectory;

    private String generatedSiteDirectory;

    private String outputDirectory;

    private String flavour;

    private  SiteRenderer siteRenderer;

    private MavenProject project;

    private MavenReportManager reportManager;

    private ArtifactRepository localRepository;

    private List remoteRepositories;

    public void execute()
        throws PluginExecutionException
    {
        try
        {
            MavenReportConfiguration config = new MavenReportConfiguration();

            config.setModel( project.getModel() );

            if ( project.getReports() != null )
            {
                reportManager.addReports( project.getReports(), localRepository, remoteRepositories );

                for ( Iterator i = project.getReports().getPlugins().iterator(); i.hasNext(); )
                {
                    org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) i.next();

                    reportManager.executeReport( plugin.getArtifactId(), config, outputDirectory );
                }
            }

            siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, flavour );
        }
        catch ( Exception e )
        {
            // TODO: handle it
            e.printStackTrace();
        }
    }
}
