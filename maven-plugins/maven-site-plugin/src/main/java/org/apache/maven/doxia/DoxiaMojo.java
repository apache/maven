package org.apache.maven.doxia;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @goal site
 * @description Doxia plugin
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class DoxiaMojo
    extends AbstractMojo
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
     * @parameter expression="${project.build.directory}/site"
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
     * @parameter expression="${reports}"
     * @required
     * @readonly
     */
    private Map reports;

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
        throws MojoExecutionException
    {
        try
        {
            MavenReportConfiguration config = new MavenReportConfiguration();

            config.setModel( project.getModel() );

            config.setOutputDirectory( new File( generatedSiteDirectory ) );
            if ( reports != null )
            {
System.out.println(reports.size());
                for ( Iterator i = reports.values().iterator(); i.hasNext(); )
                {
                    MavenReport report = (MavenReport) i.next();

                    report.setConfiguration( config );

                    report.generate();
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
