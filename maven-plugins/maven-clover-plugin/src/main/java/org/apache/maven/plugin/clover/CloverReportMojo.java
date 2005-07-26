package org.apache.maven.plugin.clover;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.cenqua.clover.reporters.html.HtmlReporter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.module.xhtml.XhtmlSink;

import java.util.Locale;
import java.util.ResourceBundle;
import java.io.File;

/**
 * Generate a Clover report.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 * @goal report
 * @execute phase="test" lifecycle="clover"
 */
public class CloverReportMojo extends AbstractMojo implements MavenReport
{
    /**
     * @parameter expression="${project.build.directory}/clover/clover.db"
     * @required
     */
    private String cloverDatabase;

    /**
     * @parameter expression="${project.build.directory}/site/clover"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private File reportOutputDirectory;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        int result = HtmlReporter.mainImpl( createCliArgs() );
        if ( result != 0 )
        {
            throw new MavenReportException( "Clover has failed to instrument the source files" );
        }
    }

    /**
     * @return the CLI args to be passed to the reporter
     * @todo handle multiple source roots. At the moment only the first source root is instrumented
     */
    private String[] createCliArgs()
    {
        return new String[]{"-t", "Maven Clover report", "-p", (String) this.project.getCompileSourceRoots().get( 0 ),
            "-i", this.cloverDatabase, "-o", this.outputDirectory};
    }

    public String getOutputName()
    {
        return "clover";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.clover.description" );
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "clover-report", locale, CloverReportMojo.class.getClassLoader() );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return this.outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return this.siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return this.project;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.clover.name" );
    }

    // The methods below are required because we don't extend AbstractMavenReport. The reason is that
    // AbstractMavenReport does not support externally generated HTML report files.

    /**
     * @see org.apache.maven.reporting.MavenReport#getReportOutputDirectory()
     */
    public File getReportOutputDirectory()
    {
        if ( this.reportOutputDirectory == null )
        {
            this.reportOutputDirectory = new File( getOutputDirectory() );
        }
        return this.reportOutputDirectory;
    }

    /**
     * @see MavenReport#setReportOutputDirectory(java.io.File)
     */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        this.reportOutputDirectory = reportOutputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_REPORTS;
    }

    /**
     * @see MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate(Sink sink, Locale locale) throws MavenReportException
    {
        executeReport( locale );
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            generate( null, Locale.ENGLISH );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
    }
}
