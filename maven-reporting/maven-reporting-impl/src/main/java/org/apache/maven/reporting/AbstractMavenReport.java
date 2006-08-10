package org.apache.maven.reporting;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * The basis for a Maven report.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractMavenReport
    extends AbstractMojo
    implements MavenReport
{
    private Sink sink;

    private SinkFactory sinkFactory;

    private Locale locale = Locale.ENGLISH;

    protected abstract Renderer getSiteRenderer();

    protected abstract String getOutputDirectory();

    protected abstract MavenProject getProject();

    private File reportOutputDirectory;

    /**
     * TODO: This method is never called - all reports are rendered by maven-site-plugin's
     * ReportDocumentRender.
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        throw new MojoExecutionException( "Reporting mojo's can only be called from ReportDocumentRender" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.apache.maven.doxia.sink.Sink, org.apache.maven.reporting.SinkFactory, java.util.Locale)
     * @deprecated
     */
    public void generate( org.codehaus.doxia.sink.Sink sink, Locale locale )
        throws MavenReportException
    {
        getLog().warn( "Deprecated API called - no SinkFactory available. Please update this plugin." );
        generate( sink, null, locale );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.apache.maven.doxia.sink.Sink, org.apache.maven.reporting.SinkFactory, java.util.Locale)
     * @deprecated
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        getLog().warn( "Deprecated API called - no SinkFactory available. Please update this plugin." );
        generate( sink, null, locale );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.apache.maven.doxia.sink.Sink, org.apache.maven.reporting.SinkFactory, java.util.Locale)
     */
    public void generate( Sink sink, SinkFactory sinkFactory, Locale locale )
        throws MavenReportException
    {
        if ( sink == null )
        {
            throw new MavenReportException( "You must specify a sink." );
        }

        this.sink = sink;

        this.sinkFactory = sinkFactory;

        executeReport( locale );

        closeReport();
    }

    protected abstract void executeReport( Locale locale )
        throws MavenReportException;

    protected void closeReport()
    {
    }

    public String getCategoryName()
    {
        return CATEGORY_PROJECT_REPORTS;
    }

    public File getReportOutputDirectory()
    {
        if ( reportOutputDirectory == null )
        {
            reportOutputDirectory = new File( getOutputDirectory() );
        }
        return reportOutputDirectory;
    }

    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        this.reportOutputDirectory = reportOutputDirectory;
    }

    public Sink getSink()
    {
        return sink;
    }

    public SinkFactory getSinkFactory()
    {
        return sinkFactory;
    }

    public boolean isExternalReport()
    {
        return false;
    }

    public boolean canGenerateReport()
    {
        return true;
    }
}
