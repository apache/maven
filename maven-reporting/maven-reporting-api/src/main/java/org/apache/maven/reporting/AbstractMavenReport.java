package org.apache.maven.reporting;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.doxia.module.xhtml.XhtmlSink;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The basis for a Maven report.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: MavenReport.java 163376 2005-02-23 00:06:06Z brett $
 */
public abstract class AbstractMavenReport
    extends AbstractMojo
    implements MavenReport
{
    /** @todo share, use default excludes from plexus utils. */
    protected static final String[] DEFAULT_EXCLUDES = {// Miscellaneous typical temporary files
        "**/*~", "**/#*#", "**/.#*", "**/%*%", "**/._*",

        // CVS
        "**/CVS", "**/CVS/**", "**/.cvsignore",

        // SCCS
        "**/SCCS", "**/SCCS/**",

        // Visual SourceSafe
        "**/vssver.scc",

        // Subversion
        "**/.svn", "**/.svn/**",

        // Mac
        "**/.DS_Store"};

    private MavenReportConfiguration config;

    private Sink sink;

    private Locale locale = Locale.ENGLISH;

    public MavenReportConfiguration getConfiguration()
    {
        return config;
    }

    public void setConfiguration( MavenReportConfiguration config )
    {
        this.config = config;
    }

    protected abstract SiteRenderer getSiteRenderer();

    protected abstract String getOutputDirectory();

    protected abstract MavenProject getProject();

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        config = new MavenReportConfiguration();

        config.setProject( getProject() );

        config.setReportOutputDirectory( new File( getOutputDirectory() ) );

        try
        {
            String outputDirectory = getOutputDirectory();

            XhtmlSink sink = getSiteRenderer().createSink( new File( outputDirectory ), getOutputName() + ".html",
                                                      outputDirectory,
                                                      getSiteDescriptor(), "maven" );

            generate( sink, Locale.ENGLISH );

            getSiteRenderer().copyResources( outputDirectory, "maven" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in " + getName( locale ) + " report generation." );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        if ( config == null )
        {
            throw new MavenReportException( "You must specify a report configuration." );
        }

        if ( sink == null )
        {
            throw new MavenReportException( "You must specify a sink configuration." );
        }
        else
        {
            this.sink = sink;
        }

        executeReport( locale );

        closeReport();
    }

    protected abstract void executeReport( Locale locale )
        throws MavenReportException;

    protected void closeReport()
    {
    }

    public Sink getSink()
        throws IOException
    {
        return sink;
    }

    public String getCategoryName()
    {
        return CATEGORY_PROJECT_REPORTS;
    }

    private String getReportsMenu()
        throws MojoExecutionException
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"Project Documentation\">\n" );

        buffer.append( "  <item name=\"" + getName( locale ) + "\" href=\"/" + getOutputName() + ".html\"/>\n" );

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    private InputStream getSiteDescriptor()
        throws MojoExecutionException
    {
        String siteDescriptorContent = "";

        try
        {
            siteDescriptorContent = IOUtil.toString( getClass().getResourceAsStream( "/default-report.xml" ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }

        Map props = new HashMap();

        props.put( "reports", getReportsMenu() );

        // TODO: interpolate ${project.*} in general

        if ( getProject().getName() != null )
        {
            props.put( "project.name", getProject().getName() );
        }
        else
        {
            props.put( "project.name", "NO_PROJECT_NAME_SET" );
        }

        if ( getProject().getUrl() != null )
        {
            props.put( "project.url", getProject().getUrl() );
        }
        else
        {
            props.put( "project.url", "NO_PROJECT_URL_SET" );
        }

        siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );

        return new StringInputStream( siteDescriptorContent );
    }
}
