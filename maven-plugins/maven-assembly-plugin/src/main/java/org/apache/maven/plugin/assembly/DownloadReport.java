package org.apache.maven.plugin.assembly;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Generates the Download report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 * @goal download
 */
public class DownloadReport
    extends AbstractMavenReport
{
    /**
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The URL of the project's download page, eg <code>http://www.apache.org/dist/maven/binaries/</code>.
     *
     * @parameter default-value="${project.distributionManagement.downloadUrl}"
     * @readonly
     */
    private String downloadUrl;

    /**
     * The directory to download the artifact in the outputDirectory.
     *
     * @parameter expression="${downloadDirectory}"
     * default-value="download"
     * @required
     */
    private String downloadDirectory;

    private List distributionFileNames = null;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.download.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.download.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * Return the download directory
     *
     * @return the download directory
     */
    protected File getDownloadDirectory()
    {
        File dir = new File( getOutputDirectory(), downloadDirectory );
        dir.mkdirs();

        return dir;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        loadDistributionFileNames();

        if ( StringUtils.isEmpty( downloadUrl ) )
        {
            try
            {
                copyDistribution();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "An IOException has occured: " + e.getMessage() );
            }
        }

        try
        {
            DownloadRenderer r = new DownloadRenderer( getSink(), getProject(), locale, ( StringUtils
                .isEmpty( downloadUrl ) ? downloadDirectory : downloadUrl ), distributionFileNames );

            r.render();
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "An error has occured: " + e.getMessage() );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "download";
    }

    /**
     * Read the assembly plugin configuration and load the distribution file names.
     *
     * @throws MavenReportException
     */
    private void loadDistributionFileNames()
        throws MavenReportException
    {
        List buildPlugins = project.getBuild().getPlugins();

        for ( Iterator it = buildPlugins.iterator(); it.hasNext(); )
        {
            Plugin expectedPlugin = (Plugin) it.next();

            if ( "maven-assembly-plugin".equals( expectedPlugin.getArtifactId() ) )
            {
                Xpp3Dom o = (Xpp3Dom) expectedPlugin.getConfiguration();

                if ( o == null )
                {
                    throw new IllegalArgumentException( "The configuration of the maven-assembly-plugin is required." );
                }

                AssemblyMojo assemblyMojo = new AssemblyMojo();

                if ( o.getChild( "finalName" ) == null )
                {
                    throw new IllegalArgumentException(
                        "The 'finalName' parameter is required for the configuration of the maven-assembly-plugin." );
                }
                assemblyMojo.finalName = o.getChild( "finalName" ).getValue();

                if ( ( o.getChild( "descriptor" ) == null ) && ( o.getChild( "descriptorId" ) == null ) )
                {
                    throw new IllegalArgumentException( "The 'descriptor' or the 'descriptorId' parameter is " +
                        "required for the configuration of the maven-assembly-plugin." );
                }
                if ( o.getChild( "descriptor" ) != null )
                {
                    File descriptor = new File( o.getChild( "descriptor" ).getValue() );
                    if ( !descriptor.exists() )
                    {
                        throw new IllegalArgumentException( "The descriptor doesn't exist." );
                    }
                    assemblyMojo.descriptor = descriptor;
                }
                if ( o.getChild( "descriptorId" ) != null )
                {
                    assemblyMojo.descriptorId = o.getChild( "descriptorId" ).getValue();
                }

                Assembly assembly;
                try
                {
                    assembly = assemblyMojo.readAssembly();
                }
                catch ( MojoFailureException e )
                {
                    throw new MavenReportException( "A MojoFailureException has occured: " + e.getMessage() );
                }
                catch ( MojoExecutionException e )
                {
                    throw new MavenReportException( "A MojoExecutionException has occured: " + e.getMessage() );
                }

                for ( Iterator it2 = assembly.getFormats().iterator(); it2.hasNext(); )
                {
                    String format = (String) it2.next();
                    if ( distributionFileNames == null )
                    {
                        distributionFileNames = new ArrayList();
                    }

                    if ( StringUtils.isEmpty( format ) )
                    {
                        throw new MavenReportException( "A none-empty format is required in the assembly descriptor." );
                    }

                    distributionFileNames.add( assemblyMojo.getDistributionName( assembly ) + "." + format );
                }
            }
        }
    }

    /**
     * Copy distribution artifacts generated by the goal assembly:assembly.
     *
     * @throws IOException if a distribution artifact is not found
     */
    private void copyDistribution()
        throws IOException
    {
        if ( ( distributionFileNames == null ) || ( distributionFileNames.isEmpty() ) )
        {
            return;
        }

        StringBuffer sb = null;

        getLog().info( "The property distributionManagement.downloadUrl is not set in the pom.xml. " +
            "Copying distribution files in a relative directory ('" + downloadDirectory + "')." );

        for ( Iterator it2 = distributionFileNames.iterator(); it2.hasNext(); )
        {
            String distName = (String) it2.next();

            File dist = new File( project.getBuild().getDirectory(), distName );
            if ( !dist.exists() )
            {
                // Not an Exception (@see AssemblyMojo#includeSite)
                if ( sb == null )
                {
                    sb = new StringBuffer();
                }
                sb.append( dist ).append( ", " );
                continue;
            }

            FileUtils.copyFileToDirectory( dist, getDownloadDirectory() );
        }

        if ( sb != null )
        {
            getLog().warn( "The " + ( distributionFileNames.size() > 1 ? "files" : "file" ) + " " +
                sb.substring( 0, sb.length() - 2 ) + " did not exist. - Please run assembly:assembly before." );
            distributionFileNames = null;
        }
    }

    static class DownloadRenderer
        extends AbstractMavenReportRenderer
    {
        private MavenProject project;

        private Locale locale;

        private String downloadUrl;

        private List distributionFileNames = null;

        /**
         * @param sink
         * @param project
         * @param locale
         * @param downloadUrl
         * @param distributionFileNames
         */
        public DownloadRenderer( Sink sink, MavenProject project, Locale locale, String downloadUrl,
                                 List distributionFileNames )
        {
            super( sink );

            this.project = project;

            this.locale = locale;

            this.downloadUrl = downloadUrl;

            this.distributionFileNames = distributionFileNames;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.download.title" ) + " " + (
                StringUtils.isEmpty( project.getName() ) ? project.getGroupId() + ":" + project.getArtifactId()
                    : project.getName() ) + " " +
                ( StringUtils.isEmpty( project.getVersion() ) ? "" : project.getVersion() );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            startSection( getTitle() );

            if ( ( distributionFileNames == null ) || ( distributionFileNames.isEmpty() ) )
            {
                paragraph( getBundle( locale ).getString( "report.download.notavailable" ) );

                endSection();

                return;
            }

            sink.paragraph();
            linkPatternedText( getBundle( locale ).getString( "report.download.intro" ) );
            sink.paragraph_();

            sink.list();
            for ( Iterator it = distributionFileNames.iterator(); it.hasNext(); )
            {
                String distName = (String) it.next();

                sink.listItem();
                sink.monospaced();

                link( downloadUrl + "/" + distName, distName.substring( distName.lastIndexOf( '.' ) ) );

                sink.monospaced_();
                sink.listItem_();
            }
            sink.list_();

            endSection();
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "assembly-plugin", locale, DownloadReport.class.getClassLoader() );
    }
}