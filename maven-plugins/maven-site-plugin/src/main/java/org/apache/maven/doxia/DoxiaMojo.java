package org.apache.maven.doxia;

import org.apache.maven.artifact.repository.ArtifactRepository;
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
 */
public class DoxiaMojo
    extends AbstractPlugin
{
    /**
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    private String siteDirectory;

    /**
     * @parameter alias="workingDirectory" expression="${project.build.directory}/site-generated"
     * @required
     */
    private String generatedSiteDirectory;

    /**
     * @parameter expressoin="${project.build.directory}/site"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter alias="flavor"
     */
    private String flavour = "maven";

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private  SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.reporting.manager.MavenReportManager}"
     * @required
     * @readonly
     */
    private MavenReportManager reportManager;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
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
