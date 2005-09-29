package org.apache.maven.plugins.site;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.siterenderer.Renderer;
import org.codehaus.plexus.siterenderer.RendererException;
import org.codehaus.plexus.siterenderer.sink.SiteRendererSink;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
public class SiteMojo
    extends AbstractMojo
{
    private static final String RESOURCE_DIR = "org/apache/maven/plugins/site";

    private static final String DEFAULT_TEMPLATE = RESOURCE_DIR + "/maven-site.vm";

    /**
     * Patterns which should be excluded by default.
     */
    // TODO Push me into a shared area (plexus-utils?)
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

        // Arch/Bazaar
        "**/.arch-ids", "**/.arch-ids/**",

        // Mac
        "**/.DS_Store"};

    /**
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    private File siteDirectory;

    /**
     * Directory containing generated documentation.
     *
     * @parameter alias="workingDirectory" expression="${project.build.directory}/generated-site"
     * @required
     */
    private File generatedSiteDirectory;

    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter expression="${project.build.directory}/site"
     * @required
     */
    private File outputDirectory;

    /**
     * Directory which contains the resources for the site.
     *
     * @parameter expression="${basedir}/src/site/resources"
     * @required
     */
    private File resourcesDirectory;

    /**
     * Directory containing the template page.
     *
     * @parameter expression="${templateDirectory}"
     */
    private String templateDirectory;

    /**
     * Default template page.
     *
     * @parameter expression="${template}"
     */
    private String template = DEFAULT_TEMPLATE;

    /**
     * @parameter expression="${attributes}"
     */
    private Map attributes;

    /**
     * A comma separated list of locales supported by Maven. The first valid token will be the default Locale
     * for this instance of the Java Virtual Machine.
     *
     * @parameter expression="${locales}"
     */
    private String locales;

    /**
     * @parameter expression="${addModules}"
     * default-value="true"
     */
    private boolean addModules;

    /**
     * @parameter expression="${outputEncoding}"
     * default-value="ISO-8859-1"
     */
    private String outputEncoding;

    /**
     * Site Renderer
     *
     * @parameter expression="${component.org.codehaus.plexus.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * Internationalization.
     *
     * @parameter expression="${component.org.codehaus.plexus.i18n.I18N}"
     * @required
     * @readonly
     */
    private I18N i18n;

    /**
     * The maven project.
     *
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
    private List reports;

    /**
     * Generate the project site
     *
     * throws MojoExecutionException if any
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( templateDirectory == null )
        {
            siteRenderer.setTemplateClassLoader( SiteMojo.class.getClassLoader() );
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

        if ( attributes == null )
        {
            attributes = new HashMap();
        }

        if ( attributes.get( "project" ) == null )
        {
            attributes.put( "project", project );
        }

        if ( attributes.get( "outputEncoding" ) == null )
        {
            attributes.put( "outputEncoding", outputEncoding );
        }

        Map categories = categorizeReports( reports );

        List projectInfos = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );
        List projectReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );

        if ( projectInfos == null )
        {
            projectInfos = Collections.EMPTY_LIST;
        }

        if ( projectReports == null )
        {
            projectReports = Collections.EMPTY_LIST;
        }

        try
        {
            List localesList = initLocalesList();
            if ( localesList.isEmpty() )
            {
                localesList = Collections.singletonList( Locale.ENGLISH );
            }

            // Default is first in the list
            Locale defaultLocale = (Locale) localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
            {
                Locale locale = (Locale) iterator.next();

                File outputDirectory = getOutputDirectory( locale, defaultLocale );

                // Safety
                if ( !outputDirectory.exists() )
                {
                    outputDirectory.mkdirs();
                }

                // Generate static site
                File siteDirectoryFile = siteDirectory;
                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );
                }

                // Try to find duplicate files
                Map duplicate = new LinkedHashMap();
                if ( siteDirectoryFile.exists() )
                {
                    tryToFindDuplicates( siteDirectoryFile, duplicate );
                }

                // Handle the GeneratedSite Directory
                if ( generatedSiteDirectory.exists() )
                {
                    tryToFindDuplicates( generatedSiteDirectory, duplicate );
                }

                // Exception if a file is duplicate
                String msg = createDuplicateExceptionMsg( duplicate, locale );
                if ( msg != null )
                {
                    throw new MavenReportException( msg );
                }

                String siteDescriptor = getSiteDescriptor( reports, locale, projectInfos, projectReports );

                //Generate reports
                List generatedReportsFileName = Collections.EMPTY_LIST;
                if ( reports != null )
                {
                    generatedReportsFileName = generateReportsPages( reports, locale, outputDirectory, defaultLocale,
                                                                     siteDescriptor );
                }

                //Generate overview pages
                if ( projectInfos.size() > 0 )
                {
                    generateProjectInfoPage( siteDescriptor, locale, projectInfos, outputDirectory );
                }

                if ( projectReports.size() > 0 )
                {
                    generateProjectReportsPage( siteDescriptor, locale, projectReports, outputDirectory );
                }

                // Try to generate the index.html
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );
                if ( duplicate.get( "index" ) != null )
                {
                    getLog().info( "Ignoring the index file generation for the " + displayLanguage + " version." );
                }
                else
                {
                    getLog().info( "Generate an index file for the " + displayLanguage + " version." );
                    generateIndexPage( siteDescriptor, locale, outputDirectory );
                }

                // Log if a user override a report file
                for ( Iterator it = generatedReportsFileName.iterator(); it.hasNext(); )
                {
                    String reportFileName = (String) it.next();

                    if ( duplicate.get( reportFileName ) != null )
                    {
                        getLog().info( "Override the generated file \"" + reportFileName + "\" for the " +
                            displayLanguage + " version." );
                    }
                }

                siteRenderer.render( siteDirectoryFile, outputDirectory, siteDescriptor, template, attributes, locale );

                File cssDirectory = new File( siteDirectory, "css" );
                File imagesDirectory = new File( siteDirectory, "images" );

                // special case for backwards compatibility
                if ( cssDirectory.exists() || imagesDirectory.exists() )
                {
                    getLog().warn( "DEPRECATED: the css and images directories are deprecated, please use resources" );

                    copyDirectory( cssDirectory, new File( outputDirectory, "css" ) );

                    copyDirectory( imagesDirectory, new File( outputDirectory, "images" ) );
                }

                copyResources( outputDirectory );

                // Copy site resources
                if ( resourcesDirectory != null && resourcesDirectory.exists() )
                {
                    copyDirectory( resourcesDirectory, outputDirectory );
                }

                // Copy the generated site in parent site if needed to provide module links
                if ( addModules )
                {
                    MavenProject parentProject = project.getParent();
                    if ( parentProject != null )
                    {
                        // TODO Handle user plugin configuration
/* TODO: Not working, and would be better working as a top-level aggregation rather than pushing from the subprojects...
                        File basedir = parentProject.getBasedir();
                        if ( basedir != null )
                        {
                            String path = parentProject.getBuild().getDirectory() + "/site/" + project.getArtifactId();
                            File parentSiteDir = new File( basedir, path );

                            if ( !parentSiteDir.exists() )
                            {
                                parentSiteDir.mkdirs();
                            }

                            File siteDir = new File( outputDirectory );
                            FileUtils.copyDirectoryStructure( siteDir, parentSiteDir );
                        }
                        else
                        {
                            getLog().info( "Not using parent as it was not located on the filesystem" );
                        }
*/
                    }
                }

                if ( generatedSiteDirectory.exists() )
                {
                    siteRenderer.render( generatedSiteDirectory, outputDirectory, siteDescriptor, template, attributes,
                                         locale );
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
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during site generation", e );
        }
    }

    /**
     * Categorize reports by category name.
     *
     * @param reports list of reports
     * @return the categorised reports
     */
    private Map categorizeReports( List reports )
    {
        Map categories = new HashMap();

        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            List category = (List) categories.get( report.getCategoryName() );

            if ( category == null )
            {
                category = new ArrayList();
                categories.put( report.getCategoryName(), category );
            }
            category.add( report );
        }
        return categories;
    }

    /**
     * Init the <code>localesList</code> variable.
     * <p>If <code>locales</code> variable is available, the first valid token will be the <code>defaultLocale</code>
     * for this instance of the Java Virtual Machine.</p>
     */
    private List initLocalesList()
    {
        if ( locales == null )
        {
            return Collections.EMPTY_LIST;
        }
        String[] localesArray = StringUtils.split( locales, "," );

        List localesList = new ArrayList();
        for ( int i = 0; i < localesArray.length; i++ )
        {
            Locale locale = codeToLocale( localesArray[i] );

            if ( locale != null )
            {
                if ( !Arrays.asList( Locale.getAvailableLocales() ).contains( locale ) )
                {
                    getLog().warn( "The locale parsed defined by '" + locale +
                        "' is not available in this Java Virtual Machine (" + System.getProperty( "java.version" ) +
                        " from " + System.getProperty( "java.vendor" ) + ") - IGNORING" );
                    continue;
                }

                if ( !i18n.getBundle( "site-plugin", locale ).getLocale().getLanguage().equals( locale.getLanguage() ) )
                {
                    StringBuffer sb = new StringBuffer();

                    sb.append( "The locale '" ).append( locale ).append( "' (" );
                    sb.append( locale.getDisplayName( Locale.ENGLISH ) );
                    sb.append( ") is not currently support by Maven - IGNORING. " );
                    sb.append( "\n" );
                    sb.append( "Contribution are welcome and greatly appreciated! " );
                    sb.append( "\n" );
                    sb.append( "If you want to contribute a new translation, please visit " );
                    sb.append( "http://maven.apache.org/maven2/plugins/maven-site-plugin/i18n.html " );
                    sb.append( "for detailed instructions." );

                    getLog().warn( sb.toString() );

                    continue;
                }

                localesList.add( locale );
            }
        }
        return localesList;
    }

    /**
     * Retrieve the reports menu
     *
     * @param locale the locale used
     * @param projectInfos list of project infos
     * @param projectReports list of project reports
     * @return a XML for reports menu
     */
    private String getReportsMenu( Locale locale, List projectInfos, List projectReports )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.projectdocumentation" ) );
        buffer.append( "\">\n" );
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

    /**
     * Create a report sub menu
     *
     * @param reports list of reports specified in pom
     * @param buffer string to be appended
     * @param locale the locale used
     * @param key
     * @param indexFilename index page filename
     */
    private void writeReportSubMenu( List reports, StringBuffer buffer, Locale locale, String key,
                                     String indexFilename )
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
     * Generate a menu for modules
     *
     * @param locale the locale wanted
     * @return a XML menu for modules
     */
    private String getModulesMenu( Locale locale )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.projectmodules" ) );
        buffer.append( "\">\n" );

        List modules = project.getModules();
        if ( project.getModules() != null )
        {
            for ( Iterator it = modules.iterator(); it.hasNext(); )
            {
                String module = (String) it.next();

                buffer.append( "    <item name=\"" );
                buffer.append( module );
                buffer.append( "\" href=\"" );
                buffer.append( module );
                buffer.append( "/index.html\"/>\n" );
            }
        }

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    /**
     * Generate a menu for the parent project
     *
     * @param locale the locale wanted
     * @return a XML menu for the parent project
     */
    private String getProjectParentMenu( Locale locale )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.parentproject" ) );
        buffer.append( "\">\n" );

        buffer.append( "    <item name=\"" );
        buffer.append( project.getParent().getArtifactId() );
        buffer.append( "\" href=\"../index.html\"/>\n" );

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    /**
     * @param reports a list of reports
     * @param locale the current locale
     * @return the inpustream
     * @throws org.apache.maven.plugin.MojoExecutionException is any
     */
    private String getSiteDescriptor( List reports, Locale locale, List projectInfos, List projectReports )
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

        props.put( "outputEncoding", outputEncoding );

        if ( reports != null )
        {
            props.put( "reports", getReportsMenu( locale, projectInfos, projectReports ) );
        }

        if ( project.getParent() != null )
        {
            /* See the Not working section*/
            //props.put( "parentProject", getProjectParentMenu( locale ) );
        }

        if ( addModules )
        {
            if ( project.getModules() != null && project.getModules().size() > 0 )
            {
                /* See the Not working section*/
                //props.put( "modules", getModulesMenu( locale ) );
            }
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

        return siteDescriptorContent;
    }

    /**
     * Generate an index page.
     *
     * @param siteDescriptor
     * @param locale
     * @param outputDirectory
     */
    private void generateIndexPage( String siteDescriptor, Locale locale, File outputDirectory )
        throws RendererException, IOException
    {
        String outputFileName = "index.html";

        SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName, siteDescriptor );

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

        File outputFile = new File( outputDirectory, outputFileName );

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       template, attributes, sink, locale );
    }

    // Generate specific pages

    /**
     * Generate reports pages
     *
     * @param reports
     * @param locale
     * @param localeOutputDirectory
     */
    private List generateReportsPages( List reports, Locale locale, File localeOutputDirectory, Locale defaultLocale,
                                       String siteDescriptor )
        throws RendererException, IOException, MavenReportException
    {
        List generatedReportsFileName = new ArrayList();

        for ( Iterator j = reports.iterator(); j.hasNext(); )
        {
            MavenReport report = (MavenReport) j.next();

            getLog().info( "Generate \"" + report.getName( locale ) + "\" report." );

            report.setReportOutputDirectory( localeOutputDirectory );

            String reportFileName = report.getOutputName();

            if ( locale.equals( defaultLocale ) )
            {
                generatedReportsFileName.add( reportFileName );
            }
            else
            {
                generatedReportsFileName.add( locale.getLanguage() + File.separator + reportFileName );
            }

            String outputFileName = reportFileName + ".html";

            SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName, siteDescriptor );

            report.generate( sink, locale );

            if ( !report.isExternalReport() )
            {
                File outputFile = new File( localeOutputDirectory, outputFileName );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                siteRenderer.generateDocument(
                    new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ), template, attributes,
                    sink, locale );
            }
        }
        return generatedReportsFileName;
    }

    /**
     * Generates Project Info Page
     *
     * @param siteDescriptor site.xml
     * @param locale the locale used
     * @param projectInfos list of projectInfos
     * @param outputDirectory directory that will contain the generated project info page
     */
    private void generateProjectInfoPage( String siteDescriptor, Locale locale, List projectInfos,
                                          File outputDirectory )
        throws RendererException, IOException
    {
        String outputFileName = "project-info.html";

        SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName, siteDescriptor );

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

        File outputFile = new File( outputDirectory, outputFileName );

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       template, attributes, sink, locale );
    }

    /**
     * Generates the Project Report Pages
     *
     * @param siteDescriptor site.xml
     * @param locale the locale used
     * @param projectReports list of project reports
     * @param outputDirectory directory that will contain the generated project report pages
     */
    private void generateProjectReportsPage( String siteDescriptor, Locale locale, List projectReports,
                                             File outputDirectory )
        throws RendererException, IOException
    {
        String outputFileName = "maven-reports.html";

        SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName, siteDescriptor );

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

        File outputFile = new File( outputDirectory, outputFileName );

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       template, attributes, sink, locale );
    }

    /**
     * Copy Resources
     *
     * @param outputDir the output directory
     * @throws IOException if any
     */
    private void copyResources( File outputDir )
        throws IOException
    {
        InputStream resourceList = getStream( RESOURCE_DIR + "/resources.txt" );

        if ( resourceList != null )
        {
            LineNumberReader reader = new LineNumberReader( new InputStreamReader( resourceList ) );

            String line = reader.readLine();

            while ( line != null )
            {
                InputStream is = getStream( RESOURCE_DIR + "/" + line );

                if ( is == null )
                {
                    throw new IOException(
                        "The resource " + line + " doesn't exists in " + DEFAULT_TEMPLATE + " template." );
                }

                File outputFile = new File( outputDir, line );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                FileOutputStream w = new FileOutputStream( outputFile );

                IOUtil.copy( is, w );

                IOUtil.close( is );

                IOUtil.close( w );

                line = reader.readLine();
            }
        }
    }

    /**
     * Get the resource as stream
     *
     * @param name
     * @return the inputstream
     */
    private InputStream getStream( String name )
    {
        return SiteMojo.class.getClassLoader().getResourceAsStream( name );
    }

    /**
     * Copy the directory
     *
     * @param source source file to be copied
     * @param destination destination file
     * @throws IOException if any
     */
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

    private File getOutputDirectory( Locale locale, Locale defaultLocale )
    {
        if ( locale.getLanguage().equals( defaultLocale.getLanguage() ) )
        {
            return outputDirectory;
        }

        return new File( outputDirectory, locale.getLanguage() );
    }

    /**
     * Convenience method that try to find duplicate files in sub-directories of a given directory.
     * <p>The scan is case sensitive.</p>
     *
     * @param directory the directory to scan
     * @param duplicate the map to update
     * @throws IOException if any
     */
    private static void tryToFindDuplicates( File directory, Map duplicate )
        throws IOException
    {
        String defaultExcludes = StringUtils.join( DEFAULT_EXCLUDES, "," );
        List siteFiles = FileUtils.getFileNames( directory, null, defaultExcludes, false );
        for ( Iterator it = siteFiles.iterator(); it.hasNext(); )
        {
            String currentFile = (String) it.next();

            if ( currentFile.lastIndexOf( File.separator ) == -1 )
            {
                // ignore files directly in the directory
                continue;
            }

            if ( currentFile.lastIndexOf( "." ) == -1 || currentFile.startsWith( "." ) )
            {
                // ignore files without extension
                continue;
            }

            String key = currentFile.substring( currentFile.indexOf( File.separator ) + 1, currentFile
                .lastIndexOf( "." ) );

            List tmp = (List) duplicate.get( key.toLowerCase() );
            if ( tmp == null )
            {
                tmp = new ArrayList();
                duplicate.put( key.toLowerCase(), tmp );
            }
            tmp.add( currentFile );
        }
    }

    /**
     * Create an <code>Exception</code> message if a file is duplicate.
     *
     * @param duplicate a map of duplicate files
     * @param locale the current locale
     * @return the Message to throw
     */
    private String createDuplicateExceptionMsg( Map duplicate, Locale locale )
    {
        if ( duplicate.size() > 0 )
        {
            StringBuffer sb = null;

            for ( Iterator it = duplicate.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                Collection values = (Collection) entry.getValue();

                if ( values.size() > 1 )
                {
                    if ( sb == null )
                    {
                        sb = new StringBuffer(
                            "Some files are duplicates in the site directory or in the generated-site directory. " );
                        sb.append( "\n" );
                        sb.append( "Review the following files for the \"" );
                        sb.append( locale.getDisplayLanguage( Locale.ENGLISH ) );
                        sb.append( "\" version:" );
                    }

                    sb.append( "\n" );
                    sb.append( entry.getKey() );
                    sb.append( "\n" );

                    for ( Iterator it2 = values.iterator(); it2.hasNext(); )
                    {
                        String current = (String) it2.next();

                        sb.append( "\t" );
                        sb.append( current );

                        if ( it2.hasNext() )
                        {
                            sb.append( "\n" );
                        }
                    }
                }
            }

            if ( sb != null )
            {
                return sb.toString();
            }
        }

        return null;
    }

    /**
     * Converts a locale code like "en", "en_US" or "en_US_win" to a <code>java.util.Locale</code>
     * object.
     * <p>If localeCode = <code>default</code>, return the current value of the default locale for this instance
     * of the Java Virtual Machine.</p>
     *
     * @param localeCode the locale code string.
     * @return a java.util.Locale object instancied or null if errors occurred
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/Locale.html">java.util.Locale#getDefault()</a>
     */
    private Locale codeToLocale( String localeCode )
    {
        if ( localeCode == null )
        {
            return null;
        }

        if ( "default".equalsIgnoreCase( localeCode ) )
        {
            return Locale.getDefault();
        }

        String language = "";
        String country = "";
        String variant = "";

        StringTokenizer tokenizer = new StringTokenizer( localeCode, "_" );
        if ( tokenizer.countTokens() > 3 )
        {
            getLog().warn( "Invalid java.util.Locale format for '" + localeCode + "' entry - IGNORING" );
            return null;
        }

        if ( tokenizer.hasMoreTokens() )
        {
            language = tokenizer.nextToken();
            if ( tokenizer.hasMoreTokens() )
            {
                country = tokenizer.nextToken();
                if ( tokenizer.hasMoreTokens() )
                {
                    variant = tokenizer.nextToken();
                }
            }
        }

        return new Locale( language, country, variant );
    }
}
