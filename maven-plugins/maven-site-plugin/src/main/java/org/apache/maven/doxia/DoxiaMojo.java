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
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
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
                for ( Iterator i = reports.keySet().iterator(); i.hasNext(); )
                {
                    String reportKey = (String) i.next();

                    getLog().info( "Generate " + reportKey + " report." );

                    MavenReport report = (MavenReport) reports.get( reportKey );

                    report.setConfiguration( config );

                    report.generate();
                }
            }

            File siteDescriptor = new File( siteDirectory, "site.xml" );
            if ( !siteDescriptor.exists() )
            {
                throw new MojoExecutionException( "The site descriptor is not present!" );
            }
            String siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
            Map props = new HashMap();
            if ( reports != null )
            {
                props.put( "reports", getReportsMenu() );
            }
            siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );
            StringInputStream siteDescriptorStream = new StringInputStream( siteDescriptorContent );

            siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, flavour,
                                 siteDescriptorStream );
        }
        catch ( Exception e )
        {
            // TODO: handle it better
            throw new MojoExecutionException( "Error during site generation", e );
        }
    }
    private String getReportsMenu()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"Project Documentation\">\n" );
        buffer.append( "    <item name=\"About " + project.getName() + "\" href=\"/index.html\"/>\n");
        buffer.append( "    <item name=\"Project reports\" href=\"/maven-reports.html\" collapse=\"true\">\n" );

        for ( Iterator i = reports.keySet().iterator(); i.hasNext(); )
        {
            String reportKey = (String) i.next();
            buffer.append( "        <item name=\"" + reportKey + "\" href=\"/" + reportKey + ".html\"/>\n" );
        }

        buffer.append( "    </item>\n" );
        buffer.append( "</menu>\n" );
        return buffer.toString();
    }
}
