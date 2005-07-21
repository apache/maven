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
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.siterenderer.Renderer;
import org.codehaus.plexus.siterenderer.RendererException;
import org.codehaus.plexus.siterenderer.sink.SiteRendererSink;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Generates the project site.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal site
 * @requiresDependencyResolution test
 */
public class DoxiaMojo
    extends AbstractMojo
{
    private static final String RESOURCE_DIR = "org/apache/maven/doxia";

    private static final String DEFAULT_TEMPLATE = RESOURCE_DIR + "/maven-site.vm";

    /**
     * Patterns which should be excluded by default.
     */
    private static final String[] DEFAULT_EXCLUDES = new String[]{
        // Miscellaneous typical temporary files
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

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

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
     * @parameter expression="${basedir}/src/site/resources"
     * @required
     */
    private File resourcesDirectory;

    /**
     * @parameter expression="${templateDirectory}
     */
    private String templateDirectory;

    /**
     * @parameter expression="${template}
     */
    private String template = DEFAULT_TEMPLATE;

    /**
     * @parameter expression="${attributes}
     */
    private Map attributes;

    /**
     * @parameter expression="${locales}
     */
    private String locales;

    /**
     * @parameter expression="${component.org.codehaus.plexus.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * @parameter expression="${component.org.codehaus.plexus.i18n.I18N}"
     * @required
     * @readonly
     */
    private I18N i18n;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.plugin.PluginManager}"
     * @required
     * @readonly
     */
    private PluginManager pluginManager;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    private List projectInfos = new ArrayList();

    private List projectReports = new ArrayList();

    private Locale defaultLocale = Locale.ENGLISH;

    private List localesList = new ArrayList();

    public void execute()
        throws MojoExecutionException
    {
        if ( templateDirectory == null )
        {
            siteRenderer.setTemplateClassLoader( DoxiaMojo.class.getClassLoader() );
        }
        else
        {
            try
            {
                URL templateDirectoryUrl = new URL( templateDirectory );

                URL[] urls = {templateDirectoryUrl};

                URLClassLoader urlClassloader = new URLClassLoader( urls );

                siteRenderer.setTemplateClassLoader( urlClassloader );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( templateDirectory + " isn't a valid URL.", e );
            }
        }

        List reports = getReports();

        try
        {
            categorizeReports( reports );

            if ( locales == null )
            {
                localesList.add( defaultLocale );
            }
            else
            {
                StringTokenizer st = new StringTokenizer( locales, "," );

                while ( st.hasMoreTokens() )
                {
                    localesList.add( new Locale( st.nextToken().trim() ) );
                }
            }

            for ( Iterator i = localesList.iterator(); i.hasNext(); )
            {
                Locale locale = (Locale) i.next();

                File localeOutputDirectory = getOuputDirectory( locale );

                // Safety
                if ( !localeOutputDirectory.exists() )
                {
                    localeOutputDirectory.mkdirs();
                }

                //Generate reports
                if ( reports != null )
                {
                    for ( Iterator j = reports.iterator(); j.hasNext(); )
                    {
                        MavenReport report = (MavenReport) j.next();

                        getLog().info( "Generate \"" + report.getName( locale ) + "\" report." );

                        report.setReportOutputDirectory( localeOutputDirectory );

                        String outputFileName = report.getOutputName() + ".html";

                        SiteRendererSink sink = siteRenderer.createSink( new File( siteDirectory ), outputFileName,
                                                                         getSiteDescriptor( reports, locale ) );

                        report.generate( sink, locale );

                        File outputFile = new File( localeOutputDirectory, outputFileName );

                        if ( !outputFile.getParentFile().exists() )
                        {
                            outputFile.getParentFile().mkdirs();
                        }

                        siteRenderer.generateDocument( new FileWriter( outputFile ), template, attributes, sink,
                                                       locale );
                    }
                }

                //Generate overview pages
                if ( projectInfos.size() > 0 )
                {
                    try
                    {
                        generateProjectInfoPage( getSiteDescriptor( reports, locale ), locale );
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
                        generateProjectReportsPage( getSiteDescriptor( reports, locale ), locale );
                    }
                    catch ( Exception e )
                    {
                        throw new MojoExecutionException( "An error is occurred in project reports page generation.",
                                                          e );
                    }
                }

                // Handle the GeneratedSite Directory
                File generatedSiteFile = new File( generatedSiteDirectory );
                if ( generatedSiteFile.exists() )
                {
                    siteRenderer.render( generatedSiteFile, localeOutputDirectory, getSiteDescriptor( reports, locale ),
                                         template, attributes, locale );
                }

                // Generate static site
                File siteDirectoryFile;

                Locale firstLocale = (Locale) localesList.get( 0 );

                if ( locale.equals( firstLocale ) )
                {
                    siteDirectoryFile = new File( siteDirectory );
                }
                else
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );
                }

                // Try to generate the index.html
                if ( !indexExists( siteDirectoryFile ) )
                {
                    getLog().info( "Generate an index file." );
                    generateIndexPage( getSiteDescriptor( reports, locale ), locale );
                }
                else
                {
                    getLog().info( "Ignoring the index file generation." );
                }

                siteRenderer.render( siteDirectoryFile, localeOutputDirectory, getSiteDescriptor( reports, locale ),
                                     template, attributes, locale );

                siteRenderer.render( siteDirectoryFile, localeOutputDirectory, getSiteDescriptor( reports, locale ),
                                     template, attributes, locale );

                File cssDirectory = new File( siteDirectory, "css" );
                File imagesDirectory = new File( siteDirectory, "images" );

                // special case for backwards compatibility
                if ( cssDirectory.exists() || imagesDirectory.exists() )
                {
                    getLog().warn( "DEPRECATED: the css and images directories are deprecated, please use resources" );

                    copyDirectory( cssDirectory, new File( localeOutputDirectory, "css" ) );

                    copyDirectory( imagesDirectory, new File( localeOutputDirectory, "images" ) );
                }

                copyResources( localeOutputDirectory );

                // Copy site resources
                if ( resourcesDirectory != null && resourcesDirectory.exists() )
                {
                    copyDirectory( resourcesDirectory, localeOutputDirectory );
                }
            }
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "Error during report generation", e );
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( "Error during page generation", e );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error during site generation", e );
        }
    }

    private void categorizeReports( List reports )
        throws MojoExecutionException
    {
        for ( Iterator i = reports.iterator(); i.hasNext(); )
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
                    report.getName( defaultLocale ) + " mojo isn't valid." );
            }
        }
    }

    private String getReportsMenu( Locale locale )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"Project Documentation\">\n" );
        buffer.append( "    <item name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.about" ) );
        buffer.append( " " );
        buffer.append( project.getName() );
        buffer.append( "\" href=\"/index.html\"/>\n" );

        writeReportSubMenu( projectInfos, buffer, locale, "report.menu.projectinformation", "project-info.html" );
        writeReportSubMenu( projectReports, buffer, locale, "report.menu.projectreports", "maven-reports.html" );

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    private void writeReportSubMenu( List reports, StringBuffer buffer, Locale locale, String key, String indexFilename )
    {
        if ( reports.size() > 0 )
        {
            buffer.append( "    <item name=\"" );
            buffer.append( i18n.getString( "site-plugin", locale, key ) );
            buffer.append( "\" href=\"/" );
            buffer.append( indexFilename );
            buffer.append( "\" collapse=\"true\">\n" );

            for ( Iterator i = reports.iterator(); i.hasNext(); )
            {
                MavenReport report = (MavenReport) i.next();
                buffer.append( "        <item name=\"" );
                buffer.append( report.getName( locale ) );
                buffer.append( "\" href=\"/" );
                buffer.append( report.getOutputName() );
                buffer.append( ".html\"/>\n" );
            }

            buffer.append( "    </item>\n" );
        }
    }

    /**
     * @todo should only be needed once
     */
    private InputStream getSiteDescriptor( List reports, Locale locale )
        throws MojoExecutionException
    {
        File siteDescriptor = new File( siteDirectory, "site_" + locale.getLanguage() + ".xml" );

        String siteDescriptorContent;

        try
        {
            if ( siteDescriptor.exists() )
            {
                siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
            }
            else
            {
                siteDescriptor = new File( siteDirectory, "site.xml" );

                if ( siteDescriptor.exists() )
                {
                    siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
                }
                else
                {
                    siteDescriptorContent = IOUtil.toString( getClass().getResourceAsStream( "/default-site.xml" ) );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }

        Map props = new HashMap();

        if ( reports != null )
        {
            props.put( "reports", getReportsMenu( locale ) );
        }

        // TODO: interpolate ${project.*} in general

        if ( project.getName() != null )
        {
            props.put( "project.name", project.getName() );
        }
        else
        {
            props.put( "project.name", "NO_PROJECT_NAME_SET" );
        }

        if ( project.getUrl() != null )
        {
            props.put( "project.url", project.getUrl() );
        }
        else
        {
            props.put( "project.url", "NO_PROJECT_URL_SET" );
        }

        siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );

        return new StringInputStream( siteDescriptorContent );
    }

    /**
     * Try to find a file called "index" in each sub-directory from the site directory.
     * We don't care about the extension.
     *
     * @param siteDirectoryFile the site directory
     * @return true if an index file was found, false otherwise
     * @throws Exception if any
     */
    private boolean indexExists( File siteDirectoryFile )
        throws Exception
    {
        getLog().debug( "Try to find an index file in the directory=[" + siteDirectoryFile + "]" );

        File[] directories = siteDirectoryFile.listFiles( new FileFilter()
        {
            public boolean accept( File file )
            {
                for ( int i = 0; i < DEFAULT_EXCLUDES.length; i++ )
                {
                    if ( SelectorUtils.matchPath( DEFAULT_EXCLUDES[i], file.getName() ) )
                    {
                        return false;
                    }
                }

                return file.isDirectory();
            }
        } );

        if ( directories == null || directories.length == 0 )
        {
            return false;
        }

        List indexFound = new ArrayList();
        for ( int i = 0; i < directories.length; i++ )
        {
            List indexes = FileUtils.getFiles( directories[i], "index.*", null, true );

            if ( indexes.size() > 1 )
            {
                getLog().warn(
                    "More than one index file exists in this directory [" + directories[i].getAbsolutePath() + "]." );
                continue;
            }

            if ( indexes.size() == 1 )
            {
                getLog().debug( "Found [" + indexes.get( 0 ) + "]" );

                indexFound.add( indexes.get( 0 ) );
            }
        }

        if ( indexFound.size() > 1 )
        {
            // TODO throw an Exception?
            getLog().warn( "More than one index file exists in the project site directory. Checks the result." );
            return true;
        }
        if ( indexFound.size() == 1 )
        {
            getLog().warn( "One index file was found in the project site directory." );
            return true;
        }

        return false;
    }

    /**
     * Generated an index page.
     *
     * @param siteDescriptor
     * @param locale
     * @throws Exception
     */
    private void generateIndexPage( InputStream siteDescriptor, Locale locale )
        throws Exception
    {
        String outputFileName = "index.html";

        SiteRendererSink sink = siteRenderer.createSink( new File( siteDirectory ), outputFileName, siteDescriptor );

        String title = i18n.getString( "site-plugin", locale, "report.index.title" ).trim() + " " + project.getName();

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
        if ( project.getDescription() != null )
        {
            // TODO How to handle i18n?
            sink.text( project.getDescription() );
        }
        else
        {
            sink.text( i18n.getString( "site-plugin", locale, "report.index.nodescription" ) );
        }
        sink.paragraph_();

        sink.body_();

        sink.flush();

        sink.close();

        siteRenderer.generateDocument( new FileWriter( new File( getOuputDirectory( locale ), outputFileName ) ),
                                       template, attributes, sink, locale );
    }

    private void generateProjectInfoPage( InputStream siteDescriptor, Locale locale )
        throws Exception
    {
        String outputFileName = "project-info.html";

        SiteRendererSink sink = siteRenderer.createSink( new File( siteDirectory ), outputFileName, siteDescriptor );

        String title = i18n.getString( "site-plugin", locale, "report.information.title" );

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
        sink.text( i18n.getString( "site-plugin", locale, "report.information.description1" ) + " " );
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( " " + i18n.getString( "site-plugin", locale, "report.information.description2" ) );
        sink.paragraph_();

        sink.section2();

        sink.sectionTitle2();
        sink.text( i18n.getString( "site-plugin", locale, "report.information.sectionTitle" ) );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.information.column.document" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.information.column.description" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator i = projectInfos.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            sink.tableRow();
            sink.tableCell();
            sink.link( report.getOutputName() + ".html" );
            sink.text( report.getName( locale ) );
            sink.link_();
            sink.tableCell_();
            sink.tableCell();
            sink.text( report.getDescription( locale ) );
            sink.tableCell_();
            sink.tableRow_();
        }

        sink.table_();

        sink.section2_();

        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();

        siteRenderer.generateDocument( new FileWriter( new File( getOuputDirectory( locale ), outputFileName ) ),
                                       template, attributes, sink, locale );
    }

    private void generateProjectReportsPage( InputStream siteDescriptor, Locale locale )
        throws Exception
    {
        String outputFileName = "maven-reports.html";

        SiteRendererSink sink = siteRenderer.createSink( new File( siteDirectory ), outputFileName, siteDescriptor );

        String title = i18n.getString( "site-plugin", locale, "report.project.title" );

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
        sink.text( i18n.getString( "site-plugin", locale, "report.project.description1" ) + " " );
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( ". " + i18n.getString( "site-plugin", locale, "report.project.description2" ) );
        sink.paragraph_();

        sink.section2();

        sink.sectionTitle2();
        sink.text( i18n.getString( "site-plugin", locale, "report.project.sectionTitle" ) );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.project.column.document" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.project.column.description" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator i = projectReports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            sink.tableRow();
            sink.tableCell();
            sink.link( report.getOutputName() + ".html" );
            sink.text( report.getName( locale ) );
            sink.link_();
            sink.tableCell_();
            sink.tableCell();
            sink.text( report.getDescription( locale ) );
            sink.tableCell_();
            sink.tableRow_();
        }

        sink.table_();

        sink.section2_();

        sink.section1_();

        sink.body_();

        siteRenderer.generateDocument( new FileWriter( new File( getOuputDirectory( locale ), outputFileName ) ),
                                       template, attributes, sink, locale );
    }

    private void copyResources( File outputDirectory )
        throws Exception
    {
        InputStream resourceList = getStream( RESOURCE_DIR + "/resources.txt" );

        if ( resourceList != null )
        {
            LineNumberReader reader = new LineNumberReader( new InputStreamReader( resourceList ) );

            String line;

            while ( ( line = reader.readLine() ) != null )
            {
                InputStream is = getStream( RESOURCE_DIR + "/" + line );

                if ( is == null )
                {
                    throw new IOException(
                        "The resource " + line + " doesn't exists in " + DEFAULT_TEMPLATE + " template." );
                }

                File outputFile = new File( outputDirectory, line );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                FileOutputStream w = new FileOutputStream( outputFile );

                IOUtil.copy( is, w );

                IOUtil.close( is );

                IOUtil.close( w );
            }
        }
    }

    private InputStream getStream( String name )
        throws Exception
    {
        return DoxiaMojo.class.getClassLoader().getResourceAsStream( name );
    }

    private void copyDirectory( File source, File destination )
        throws IOException
    {
        if ( source.exists() )
        {
            DirectoryScanner scanner = new DirectoryScanner();

            String[] includedResources = {"**/**"};

            scanner.setIncludes( includedResources );

            scanner.addDefaultExcludes();

            scanner.setBasedir( source );

            scanner.scan();

            List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

            for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                File sourceFile = new File( source, name );

                File destinationFile = new File( destination, name );

                FileUtils.copyFile( sourceFile, destinationFile );
            }
        }
    }

    private File getOuputDirectory( Locale locale )
    {
        if ( localesList.size() == 1 )
        {
            return new File( outputDirectory );
        }
        else
        {
            Locale firstLocale = (Locale) localesList.get( 0 );
            if ( locale.equals( firstLocale ) )
            {
                return new File( outputDirectory );
            }
            else
            {
                return new File( outputDirectory, locale.getLanguage() );
            }
        }
    }

    private List getReports()
        throws MojoExecutionException
    {
        // TODO: not the best solution. Perhaps a mojo tag that causes the plugin manager to populate project reports instead?

        List reportPlugins = project.getReportPlugins();

        if ( project.getModel().getReports() != null )
        {
            getLog().error(
                "DEPRECATED: Plugin contains a <reports/> section: this is IGNORED - please use <reporting/> instead." );
        }

        List reports = new ArrayList();
        if ( reportPlugins != null )
        {
            for ( Iterator it = reportPlugins.iterator(); it.hasNext(); )
            {
                ReportPlugin reportPlugin = (ReportPlugin) it.next();

                try
                {
                    List reportSets = reportPlugin.getReportSets();

                    List reportsList = new ArrayList();

                    if ( reportSets == null || reportSets.isEmpty() )
                    {
                        reportsList = pluginManager.getReports( reportPlugin, null, project, session, localRepository );

                    }
                    else
                    {
                        for ( Iterator j = reportSets.iterator(); j.hasNext(); )
                        {
                            ReportSet reportSet = (ReportSet) j.next();

                            reportsList = pluginManager.getReports( reportPlugin, reportSet, project, session,
                                                                    localRepository );
                        }
                    }

                    reports.addAll( reportsList );
                }
                catch ( PluginManagerException e )
                {
                    throw new MojoExecutionException( "Error getting reports", e );
                }
                catch ( PluginVersionResolutionException e )
                {
                    throw new MojoExecutionException( "Error getting reports", e );
                }
                catch ( PluginConfigurationException e )
                {
                    throw new MojoExecutionException( "Error getting reports", e );
                }
                catch ( ArtifactResolutionException e )
                {
                    throw new MojoExecutionException( "Cannot find report plugin", e );
                }
            }
        }
        return reports;
    }
}
