package org.apache.maven.report.projectinfo;

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
import org.codehaus.doxia.module.xhtml.XhtmlSink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A Maven2 plugin which generates the set of project reports.
 * @goal generate
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: ProjectReportsMojo.java,v 1.3 2005/02/23 00:08:03 brett Exp $
 */
public class ProjectReportsMojo
    extends AbstractMojo
{
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

        config.setProject( project );

        config.setReportOutputDirectory( new File( outputDirectory ) );

        MavenReport dependenciesReport = new DependenciesReport();

        dependenciesReport.setConfiguration( config );

        MavenReport mailingListsReport = new MailingListsReport();

        mailingListsReport.setConfiguration( config );

        try
        {
            XhtmlSink sink = siteRenderer.createSink( new File( siteDirectory ), siteDirectory,
                                                      dependenciesReport.getOutputName() + ".html",
                                                      outputDirectory, getSiteDescriptor(), flavour );

            dependenciesReport.generate( sink );

            sink = siteRenderer.createSink( new File( siteDirectory ), siteDirectory,
                                            mailingListsReport.getOutputName() + ".html",
                                            outputDirectory, getSiteDescriptor(), flavour );

            mailingListsReport.generate( sink );

            siteRenderer.copyResources( outputDirectory, flavour, siteDirectory );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the report generation.", e );
        }
    }

    private String getReportsMenu()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"Project Documentation\">\n" );
        buffer.append( "    <item name=\"About " + project.getName() + "\" href=\"/index.html\"/>\n");
        buffer.append( "    <item name=\"Project reports\" href=\"/maven-reports.html\" collapse=\"true\">\n" );

        buffer.append( "        <item name=\"Dependencies\" href=\"/dependencies.html\"/>\n" );
        buffer.append( "        <item name=\"Mailing list\" href=\"/mail-list.html\"/>\n" );

        buffer.append( "    </item>\n" );
        buffer.append( "</menu>\n" );
        return buffer.toString();
    }

    /** @noinspection IOResourceOpenedButNotSafelyClosed*/
    private InputStream getSiteDescriptor()
        throws MojoExecutionException
    {
        File siteDescriptor = new File( siteDirectory, "site.xml" );

        if ( !siteDescriptor.exists() )
        {
            throw new MojoExecutionException( "The site descriptor is not present!" );
        }

        String siteDescriptorContent;

        try
        {
            siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
        }
        catch( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }

        Map props = new HashMap( 1 );

        props.put( "reports", getReportsMenu() );

        siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );

        return new StringInputStream( siteDescriptorContent );
    }
}
