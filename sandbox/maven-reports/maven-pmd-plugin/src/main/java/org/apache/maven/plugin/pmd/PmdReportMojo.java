package org.apache.maven.plugin.pmd;

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
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.module.xhtml.XhtmlSink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @goal pmd
 * @description A Maven2 plugin which generates a PMD report
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: PmdReportMojo.java,v 1.4 2005/02/23 00:08:54 brett Exp $
 */
public class PmdReportMojo
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

    public void execute()
        throws MojoExecutionException
    {
        MavenReportConfiguration config = new MavenReportConfiguration();

        config.setModel( project.getModel() );

        config.setOutputDirectory( new File( outputDirectory ) );

        MavenReport report = new PmdReport();

        report.setConfiguration( config );

        try
        {
            XhtmlSink sink = siteRenderer.createSink( new File( siteDirectory ), siteDirectory,
                                                      report.getOutputName() + ".html",
                                                      outputDirectory, getSiteDescriptor(), flavour );

            report.generate( sink );

            siteRenderer.copyResources( siteDirectory, outputDirectory, flavour );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the PMD report generation.", e );
        }
    }

    private String getReportsMenu()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"Project Documentation\">\n" );
        buffer.append( "    <item name=\"About " + project.getName() + "\" href=\"/index.html\"/>\n");
        buffer.append( "    <item name=\"Project reports\" href=\"/maven-reports.html\" collapse=\"true\">\n" );

        buffer.append( "        <item name=\"PMD report\" href=\"/pmd.html\"/>\n" );

        buffer.append( "    </item>\n" );
        buffer.append( "</menu>\n" );
        return buffer.toString();
    }

    private InputStream getSiteDescriptor()
        throws MojoExecutionException
    {
        File siteDescriptor = new File( siteDirectory, "site.xml" );

        if ( !siteDescriptor.exists() )
        {
            throw new MojoExecutionException( "The site descriptor is not present!" );
        }

        String siteDescriptorContent = "";

        try
        {
            siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
        }
        catch( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }

        Map props = new HashMap();

        props.put( "reports", getReportsMenu() );

        siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );

        return new StringInputStream( siteDescriptorContent );
    }
}
