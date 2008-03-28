package tests;

import org.apache.maven.doxia.siterenderer.DefaultSiteRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.util.Locale;

/**
 * @goal report
 * @execute phase="site"
 */
public class ForkingReport
    extends AbstractMavenReport
{

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        System.out.println( "\n\n\nHI!\n\n" );
    }

    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected Renderer getSiteRenderer()
    {
        return new DefaultSiteRenderer();
    }

    public String getDescription( Locale locale )
    {
        return "test";
    }

    public String getName( Locale locale )
    {
        return "test";
    }

    public String getOutputName()
    {
        return "test";
    }

}
