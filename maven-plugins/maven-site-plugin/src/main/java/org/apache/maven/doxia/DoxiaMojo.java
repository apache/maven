package org.apache.maven.doxia;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.codehaus.doxia.module.xhtml.XhtmlSink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.IOUtil;

import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
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
     * @parameter expression="${basedir}"
     * @required
     */
    private String basedir;

    /**
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    private String siteDirectory;

    /**
     * @parameter alias="workingDirectory" expression="${project.build.directory}/generated-site"
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

    private List projectInfos = new ArrayList();
    private List projectReports = new ArrayList();

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            categorizeReports();

            MavenReportConfiguration config = new MavenReportConfiguration();

            config.setModel( project.getModel() );

            //Generate reports
            if ( reports != null )
            {
                for ( Iterator i = reports.keySet().iterator(); i.hasNext(); )
                {
                    String reportKey = (String) i.next();

                    getLog().info( "Generate " + reportKey + " report." );

                    MavenReport report = (MavenReport) reports.get( reportKey );

                    report.setConfiguration( config );

                    XhtmlSink sink = siteRenderer.createSink( new File( siteDirectory ), siteDirectory,
                                                              report.getOutputName() + ".html",
                                                              outputDirectory, getSiteDescriptor(), flavour );

                    report.generate( sink );
                }
            }

            //Generate overview pages
            if ( projectInfos.size() > 0 )
            {
                try
                {
                    generateProjectInfoPage( getSiteDescriptor() );
                }
                catch ( Exception e )
                {
                    throw new MojoExecutionException( "An error is occurred in project info page generation.", e );
                }
            }

            if ( projectReports.size() > 0 )
            {
                try
                {
                    generateProjectReportsPage( getSiteDescriptor() );
                }
                catch ( Exception e )
                {
                    throw new MojoExecutionException( "An error is occurred in project reports page generation.", e );
                }
            }

            //Generate static site
            siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, flavour, getSiteDescriptor() );
        }
        catch ( Exception e )
        {
            // TODO: handle it better
            throw new MojoExecutionException( "Error during site generation", e );
        }
    }

    private void categorizeReports()
        throws MojoExecutionException
    {
        for ( Iterator i = reports.values().iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();
            if ( MavenReport.CATEGORY_PROJECT_INFORMATION.equals( report.getCategoryName() ) )
            {
                projectInfos.add( report );
            }
            else if ( MavenReport.CATEGORY_PROJECT_REPORTS.equals( report.getCategoryName() ) )
            {
                projectReports.add( report );
            }
            else
            {
                throw new MojoExecutionException( "'" + report.getCategoryName() + "' category define for " +
                                                  report.getName() + " mojo isn't valid." );
            }
        }
    }

    private String getReportsMenu()
        throws MojoExecutionException
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"Project Documentation\">\n" );
        buffer.append( "    <item name=\"About " + project.getName() + "\" href=\"/index.html\"/>\n");

        if ( projectInfos.size() > 0 )
        {
            buffer.append( "    <item name=\"" + MavenReport.CATEGORY_PROJECT_INFORMATION +
                           "\" href=\"/project-info.html\" collapse=\"true\">\n" );

            for ( Iterator i = projectInfos.iterator(); i.hasNext(); )
            {
                MavenReport report = (MavenReport) i.next();
                buffer.append( "        <item name=\"" + report.getName() + "\" href=\"/" +
                               report.getOutputName() + ".html\"/>\n" );
            }

            buffer.append( "    </item>\n" );
        }

        if ( projectReports.size() > 0 )
        {
            buffer.append( "    <item name=\"" + MavenReport.CATEGORY_PROJECT_REPORTS +
                           "\" href=\"/maven-reports.html\" collapse=\"true\">\n" );

            for ( Iterator i = projectReports.iterator(); i.hasNext(); )
            {
                MavenReport report = (MavenReport) i.next();
                buffer.append( "        <item name=\"" + report.getName() + "\" href=\"/" +
                               report.getOutputName() + ".html\"/>\n" );
            }

            buffer.append( "    </item>\n" );
        }

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    private InputStream getSiteDescriptor()
        throws MojoExecutionException
    {
        File siteDescriptor = new File( siteDirectory, "site.xml" );

        String siteDescriptorContent = "";

        try
        {
            if ( siteDescriptor.exists() )
            {
                siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
            }
            else
            {
                siteDescriptorContent = IOUtil.toString( getClass().getResourceAsStream( "/default-site.xml" ) );
            }
        }
        catch( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }

        Map props = new HashMap();

        if ( reports != null )
        {
            props.put( "reports", getReportsMenu() );
        }

        // TODO: interpolate ${project.*} in general
        props.put( "project.name", project.getName() );
        props.put( "project.url", project.getUrl() );

        siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );

        return new StringInputStream( siteDescriptorContent );
    }

    private void generateProjectInfoPage( InputStream siteDescriptor )
        throws Exception
    {
        XhtmlSink sink = siteRenderer.createSink( new File( siteDirectory ), siteDirectory,
                                                  "project-info.html",
                                                  outputDirectory, siteDescriptor, flavour );

        String title = "General Project Information";

        sink.head();
        sink.title();
        sink.text( title );
        sink.title_();
        sink.head_();
        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( title );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( "This document provides an overview of the various documents and links that are part " +
                   "of this project's general information. All of this content is automatically generated by ");
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( " on behalf of the project." );
        sink.paragraph_();

        sink.section2();

        sink.sectionTitle2();
        sink.text( "Overview" );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( "Document" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Description" );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator i = projectInfos.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            sink.tableRow();
            sink.tableCell();
            sink.link( report.getOutputName() + ".html" );
            sink.text( report.getName() );
            sink.link_();
            sink.tableCell_();
            sink.tableCell();
            sink.text( report.getDescription() );
            sink.tableCell_();
            sink.tableRow_();
        }

        sink.table_();
        
        sink.section2_();

        sink.section1_();

        sink.body_();
    }

    private void generateProjectReportsPage( InputStream siteDescriptor)
        throws Exception
    {
        XhtmlSink sink = siteRenderer.createSink( new File( siteDirectory ), siteDirectory,
                                                  "maven-reports.html",
                                                  outputDirectory, siteDescriptor, flavour );

        String title = "Maven Generated Reports";

        sink.head();
        sink.title();
        sink.text( title );
        sink.title_();
        sink.head_();
        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( title );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( "This document provides an overview of the various reports that are automatically generated by " );
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( ". Each report is briefly described below." );
        sink.paragraph_();

        sink.section2();

        sink.sectionTitle2();
        sink.text( "Overview" );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( "Document" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Description" );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator i = projectReports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            sink.tableRow();
            sink.tableCell();
            sink.link( report.getOutputName() + ".html" );
            sink.text( report.getName() );
            sink.link_();
            sink.tableCell_();
            sink.tableCell();
            sink.text( report.getDescription() );
            sink.tableCell_();
            sink.tableRow_();
        }

        sink.table_();
        
        sink.section2_();

        sink.section1_();

        sink.body_();
    }
}
