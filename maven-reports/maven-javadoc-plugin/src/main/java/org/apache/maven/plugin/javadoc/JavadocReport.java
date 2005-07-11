package org.apache.maven.plugin.javadoc;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.model.Model;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * This class provides the Javadoc report support.
 * 
 * @goal javadoc
 * @requiresDependencyResolution compile
 * 
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * 
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 * 
 * @see <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a>
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#options">Javadoc Options </a>
 */
public class JavadocReport
    extends AbstractMavenReport
{
    /** Default organization name */
    private static final String DEFAULT_ORGANIZATION_NAME = "The Apache Software Foundation";

    /** Default location for css */
    private static final String DEFAULT_STYLESHEET_LOCATION = "org/apache/maven/plugin/javadoc/css/stylesheet.css";

    /**
     * @parameter expression="${project.build.directory}/site"
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

    // JavaDoc Options
    // @see http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#options
    /**
     * Set an additional parameter on the command line
     * 
     * @parameter expression="${additionalparam}"
     */
    private String additionalparam;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#breakiterator">breakiterator</a>
     * 
     * @parameter expression="${breakiterator}"
     */
    private boolean breakiterator = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doclet">doclet</a>
     * 
     * @parameter expression="${doclet}"
     */
    private String doclet;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>
     * 
     * @parameter expression="${docletPath}"
     */
    private String docletPath;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#encoding">encoding</a>
     * 
     * @parameter expression="${encoding}"
     */
    private String encoding;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#exclude">exclude</a>
     * 
     * @parameter expression="${excludePackageNames}"
     */
    private String excludePackageNames;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#extdirs">extdirs</a>
     * 
     * @parameter expression="${extdirs}"
     */
    private String extdirs;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#locale">locale</a>
     * 
     * @parameter expression="${locale}"
     */
    private String locale;

    /**
     * Set the maximum memory to be used by the javadoc process
     * 
     * @parameter expression="${maxmemory}"
     */
    private String maxmemory;

    /**
     * Set the minimum memory to be used by the javadoc process
     * 
     * @parameter expression="${minmemory}"
     */
    private String minmemory;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#1.1">1.1</a>
     * 
     * @parameter expression="${old}"
     */
    private boolean old = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>
     * 
     * @parameter expression="${overview}"
     */
    private String overview;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#package">package</a>
     * 
     * @parameter expression="${package}"
     */
    private boolean package_ = true;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#private">private</a>
     * 
     * @parameter expression="${private}"
     */
    private boolean private_ = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#protected">protected</a>
     * 
     * @parameter expression="${protected}"
     */
    private boolean protected_ = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#public">public</a>
     * 
     * @parameter expression="${public}"
     */
    private boolean public_ = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#quiet">quiet</a>
     * 
     * @parameter expression="${quiet}"
     */
    private boolean quiet = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#source">source</a>
     * 
     * @parameter expression="${source}"
     */
    private String source;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#verbose">verbose</a>
     * 
     * @parameter expression="${verbose}"
     */
    private boolean verbose = false;

    // Options Provided by the Standard Doclet
    // @see http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#standard
    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#author">author</a>
     * 
     * @parameter expression="${author}"
     */
    private boolean author = true;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#bottom">bottom</a>
     * 
     * @parameter expression="${bottom}"
     */
    private String bottom;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#charset">charset</a>
     * 
     * @parameter expression="${charset}"
     */
    private String charset = "ISO-8859-1";

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#d">d</a>
     * 
     * @parameter expression="${destDir}"
     */
    private String destDir;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docfilessubdirs">docfilessubdirs</a>
     * 
     * @parameter expression="${docfilessubdirs}"
     */
    private boolean docfilessubdirs = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docencoding">docencoding</a>
     * 
     * @parameter expression="${docencoding}"
     */
    private String docencoding;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doctitle">doctitle</a>
     * 
     * @parameter expression="${doctitle}"
     */
    private String doctitle;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#excludedocfilessubdir">excludedocfilessubdir</a>
     * 
     * @parameter expression="${excludedocfilessubdir}"
     */
    private String excludedocfilessubdir;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#footer">footer</a>
     * 
     * @parameter expression="${footer}"
     */
    private String footer;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#group">group</a>
     * 
     * @parameter expression="${group}"
     */
    private String group;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#header">header</a>
     * 
     * @parameter expression="${header}"
     */
    private String header;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#helpfile">helpfile</a>
     * 
     * @parameter expression="${helpfile}"
     */
    private String helpfile;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#link">link</a>
     * 
     * @parameter expression="${link}"
     */
    private String link;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linkoffline">linkoffline</a>
     * 
     * @parameter expression="${linkoffline}"
     */
    private String linkoffline;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linksource">linksource</a>
     * 
     * @parameter expression="${linksource}"
     */
    private boolean linksource = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linksource">nodeprecated</a>
     * 
     * @parameter expression="${nodeprecated}"
     */
    private boolean nodeprecated = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nocomment">nocomment</a>
     * 
     * @parameter expression="${nocomment}"
     */
    private boolean nocomment = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecatedlist">nodeprecatedlist</a>
     * 
     * @parameter expression="${nodeprecatedlist}"
     */
    private boolean nodeprecatedlist = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nohelp">nohelp</a>
     * 
     * @parameter expression="${nohelp}"
     */
    private boolean nohelp = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noindex">noindex</a>
     * 
     * @parameter expression="${noindex}"
     */
    private boolean noindex = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nonavbar">nonavbar</a>
     * 
     * @parameter expression="${nonavbar}"
     */
    private boolean nonavbar = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noqualifier">noqualifier</a>
     * 
     * @parameter expression="${noqualifier}"
     */
    private String noqualifier;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nosince">nosince</a>
     * 
     * @parameter expression="${nosince}"
     */
    private boolean nosince = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#notree">notree</a>
     * 
     * @parameter expression="${notree}"
     */
    private boolean notree = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#serialwarn">serialwarn</a>
     * 
     * @parameter expression="${serialwarn}"
     */
    private boolean serialwarn = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#splitindex">splitindex</a>
     * 
     * @parameter expression="${splitindex}"
     */
    private boolean splitindex = false;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#stylesheetfile">stylesheetfile</a>
     * 
     * @parameter expression="${stylesheetfile}"
     */
    private String stylesheetfile;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tag">tag</a>
     * 
     * @parameter expression="${tag}"
     */
    private String tag;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>
     * 
     * @parameter expression="${taglet}"
     */
    private String taglet;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>
     * 
     * @parameter expression="${tagletpath}"
     */
    private String tagletpath;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#use">use</a>
     * 
     * @parameter expression="${use}"
     */
    private boolean use = true;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#version">version</a>
     * 
     * @parameter expression="${version}"
     */
    private boolean version = true;

    /**
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>
     * 
     * @parameter expression="${windowtitle}"
     */
    private String windowtitle;
    // End JavaDoc parameters

    /** The command line built to execute Javadoc. */
    private Commandline cmd = new Commandline();

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return "JavaDocs";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        // TODO i18n
        return "JavaDoc API documentation.";
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
     * @see org.apache.maven.reporting.MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        executeReport( locale );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        try
        {
            File outputDir = new File( getReportOutputDirectory().getAbsolutePath() + "/apidocs" );
            outputDir.mkdirs();

            int actualYear = Calendar.getInstance().get( Calendar.YEAR );
            String year = String.valueOf( actualYear );

            Model model = getProject().getModel();
            if ( model.getInceptionYear() != null )
            {
                if ( StringUtils.isNumeric( model.getInceptionYear() ) )
                {
                    if ( Integer.valueOf( model.getInceptionYear() ).intValue() != actualYear )
                    {
                        year = model.getInceptionYear() + "-" + String.valueOf( actualYear );
                    }
                }
                else
                {
                    getLog().warn( "The inception year is not a valid year." );
                }
            }

            StringBuffer classpath = new StringBuffer();
            for ( Iterator i = getProject().getCompileClasspathElements().iterator(); i.hasNext(); )
            {
                classpath.append( (String) i.next() );
                if ( i.hasNext() )
                {
                    classpath.append( ";" );
                }
            }

            StringBuffer sourcePath = new StringBuffer();
            String[] fileList = new String[1];
            for ( Iterator i = getProject().getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String sourceDirectory = (String) i.next();
                fileList = FileUtils.getFilesFromExtension( sourceDirectory, new String[] { "java" } );
                sourcePath.append( sourceDirectory );
            }

            File javadocDirectory = new File( getProject().getBuild().getDirectory() + "/javadoc" );
            if ( fileList != null && fileList.length != 0 )
            {
                StringBuffer files = new StringBuffer();
                for ( int i = 0; i < fileList.length; i++ )
                {
                    files.append( fileList[i] );
                    files.append( "\n" );
                }
                javadocDirectory.mkdirs();
                FileUtils.fileWrite( new File( javadocDirectory, "files" ).getAbsolutePath(), files.toString() );
            }
            else
            {
                return;
            }

            // Copy default style sheet
            copyDefaultStylesheet( javadocDirectory );

            this.cmd.setWorkingDirectory( javadocDirectory.getAbsolutePath() );
            this.cmd.setExecutable( getJavadocPath() );

            // General javadoc arguments
            addArgIf( this.breakiterator, "-breakiterator", 1.4f );
            if ( !StringUtils.isEmpty( this.doclet ) )
            {
                addArgIfNotEmpty( "-doclet", this.doclet );
                addArgIfNotEmpty( "-docletPath", this.docletPath );
            }
            addArgIfNotEmpty( "-encoding", this.encoding );
            addArgIfNotEmpty( "-extdirs", this.extdirs );
            addArgIfNotEmpty( "-exclude", this.excludePackageNames, 1.4f );
            addArgIfNotEmpty( "-locale", this.locale );
            if ( !StringUtils.isEmpty( this.maxmemory ) )
            {
                // Allow '128' or '128m'
                if ( NumberUtils.isDigits( this.maxmemory ) )
                {
                    addArgIf( true, "-J-Xmx" + this.maxmemory + "m" );
                }
                else
                {
                    if ( ( NumberUtils.isDigits( this.maxmemory.substring( 0, this.maxmemory.length() - 1) ) ) 
                        && ( this.maxmemory.toLowerCase().endsWith( "m" ) ) ) 
                    {

                        addArgIf( true, "-J-Xmx" + this.maxmemory );
                    }
                    else
                    {
                        getLog().error( "The maxmemory '" + this.maxmemory + "' is not a valid number. Ignore this option." );
                    }
                }
            }
            if ( !StringUtils.isEmpty( this.minmemory ) )
            {
                // Allow '128' or '128m'
                if ( NumberUtils.isDigits( this.minmemory ) )
                {
                    addArgIf( true, "-J-Xms" + this.minmemory + "m" );
                }
                else
                {
                    if ( ( NumberUtils.isDigits( this.minmemory.substring( 0, this.minmemory.length() - 1) ) ) 
                        && ( this.minmemory.toLowerCase().endsWith( "m" ) ) ) 
                    {

                        addArgIf( true, "-J-Xms" + this.minmemory );
                    }
                    else
                    {
                        getLog().error( "The minmemory '" + this.minmemory + "' is not a valid number. Ignore this option." );
                    }
                }
            }
            if ( this.old && SystemUtils.isJavaVersionAtLeast( 1.4f ) )
            {
                getLog().warn( "Javadoc 1.4 doesn't support the -1.1 switch anymore. Ignore this option." );
            }
            else
            {
                addArgIf( this.old, "-1.1" );
            }
            addArgIfNotEmpty( "-overview", this.overview );
            addArgIf( this.package_, "-package" );
            addArgIf( this.private_, "-private" );
            addArgIf( this.protected_, "-protected" );
            addArgIf( this.public_, "-public" );
            addArgIf( this.quiet, "-quiet", 1.4f );
            addArgIfNotEmpty( "-source", this.source, 1.4f );
            addArgIf( this.verbose, "-verbose" );
            addArgIfNotEmpty( "-additionalparam", this.additionalparam );

            addArgIfNotEmpty( "-sourcePath", sourcePath.toString() );
            addArgIfNotEmpty( "-classpath", classpath.toString() );

            // javadoc arguments for default doclet
            if ( StringUtils.isEmpty( this.doclet ) )
            {
                // Specify default values
                if ( StringUtils.isEmpty( this.bottom ) )
                {
                    this.bottom = "Copyright &copy; " + year + " ";

                    if ( ( model.getOrganization() != null ) && ( !StringUtils.isEmpty( model.getOrganization().getName() ) ) )
                    {
                        this.bottom += model.getOrganization().getName();
                    }
                    else
                    {
                        this.bottom += DEFAULT_ORGANIZATION_NAME;
                    }
                    this.bottom += ". All Rights Reserved.";
                }
                if ( StringUtils.isEmpty( this.destDir ) )
                {
                    this.destDir = outputDir.getAbsolutePath();
                }
                if ( StringUtils.isEmpty( this.stylesheetfile ) )
                {
                    this.stylesheetfile = javadocDirectory + File.separator + DEFAULT_STYLESHEET_LOCATION;
                }
                if ( StringUtils.isEmpty( this.windowtitle ) )
                {
                    this.windowtitle = ( model.getName() == null ? model.getArtifactId() : model.getName() ) + " "
                        + model.getVersion() + " API";
                }
                if ( StringUtils.isEmpty( this.doctitle ) )
                {
                    this.doctitle = this.windowtitle;
                }
                // End Specify default values

                addArgIf( this.author, "-author" );
                addArgIfNotEmpty( "-bottom", this.bottom );
                addArgIf( this.breakiterator, "-breakiterator", 1.4f );
                addArgIfNotEmpty( "-charset", this.charset );
                addArgIfNotEmpty( "-d", this.destDir );
                addArgIf( this.docfilessubdirs, "-docfilessubdirs", 1.4f );
                addArgIfNotEmpty( "-docencoding", this.docencoding );
                addArgIfNotEmpty( "-doctitle", this.doctitle );
                addArgIfNotEmpty( "-excludePackageNames", this.excludePackageNames );
                addArgIfNotEmpty( "-excludedocfilessubdir", this.excludedocfilessubdir, 1.4f );
                addArgIfNotEmpty( "-footer", this.footer );
                addArgIfNotEmpty( "-group", this.group );
                addArgIfNotEmpty( "-header", this.header );
                addArgIfNotEmpty( "-helpfile", this.helpfile );
                addArgIfNotEmpty( "-link", this.link );
                addArgIfNotEmpty( "-linkoffline", this.linkoffline );
                addArgIf( this.linksource, "-linksource", 1.4f );
                addArgIf( this.nodeprecated, "-nodeprecated" );
                addArgIf( this.nodeprecatedlist, "-nodeprecatedlist" );
                addArgIf( this.nocomment, "-nocomment", 1.4f );
                addArgIf( this.nohelp, "-nohelp" );
                addArgIf( this.noindex, "-noindex" );
                addArgIf( this.nonavbar, "-nonavbar" );
                addArgIfNotEmpty( "-noqualifier", this.noqualifier, 1.4f );
                addArgIf( this.nosince, "-nosince" );
                addArgIf( this.notree, "-notree" );
                addArgIf( this.serialwarn, "-serialwarn" );
                addArgIf( this.splitindex, "-splitindex" );
                addArgIfNotEmpty( "-stylesheetfile", this.stylesheetfile );
                addArgIfNotEmpty( "-tag", this.tag, 1.4f );
                addArgIfNotEmpty( "-taglet", this.taglet, 1.4f );
                addArgIfNotEmpty( "-tagletpath", this.tagletpath, 1.4f );
                addArgIf( this.use, "-use" );
                addArgIf( this.version, "-version" );
                addArgIfNotEmpty( "-windowtitle", this.windowtitle );
            }

            cmd.createArgument().setValue( "@files" );

            getLog().info( Commandline.toString( cmd.getCommandline() ) );

            final int exitCode = CommandLineUtils.executeCommandLine( cmd, new DefaultConsumer(), new DefaultConsumer() );
            if ( exitCode != 0 )
            {
                throw new MavenReportException( "Exit code: " + exitCode );
            }
        }
        catch ( Exception e )
        {
            getLog().debug( e );
            throw new MavenReportException( "An error has occurred in javadoc report generation.", e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "apidocs/index";
    }

    /**
     * Get the path of Javadoc tool depending the OS.
     * 
     * @return the path of the Javadoc tool
     */
    private String getJavadocPath()
    {
        final String javadocCommand = "javadoc" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );
        // For IBM's JDK 1.2
        final File javadocExe = ( SystemUtils.IS_OS_AIX ? new File( SystemUtils.getJavaHome() + "/../sh", javadocCommand )
                                                  : new File( SystemUtils.getJavaHome() + "/../bin", javadocCommand ) );

        getLog().debug( "Javadoc executable=[" + javadocExe.getAbsolutePath() + "]" );
        
        return javadocExe.getAbsolutePath();
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * conditionally based on the given flag.
     * 
     * @param b
     *            the flag which controls if the argument is added or not.
     * @param value
     *            the argument value to be added.
     */
    private void addArgIf( final boolean b, final String value )
    {
        if ( b )
        {
            this.cmd.createArgument().setValue( value );
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     * 
     * @see #addArgIf(boolean, String)
     * @see <a href="http://jakarta.apache.org/commons/lang/api/org/apache/commons/lang/SystemUtils.html#isJavaVersionAtLeast(float)">SystemUtils.html#isJavaVersionAtLeast(float)</a>
     * 
     * @param b
     *            the flag which controls if the argument is added or not.
     * @param value
     *            the argument value to be added.
     * @param requiredJavaVersion  
     *            the required Java version, for example 1.31f or 1.4f
     */
    private void addArgIf( final boolean b, final String value, final float requiredJavaVersion )
    {
        if ( SystemUtils.isJavaVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIf( b, value );
        }
        else
        {
            getLog().warn( value + " option is not supported on Java version < " + requiredJavaVersion );
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p>
     * Moreover, the value could be comma separated.
     * 
     * @param key
     *            the argument name.
     * @param value
     *            the argument value to be added.
     */
    private void addArgIfNotEmpty( final String key, final String value )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            this.cmd.createArgument().setValue( key );

            StringTokenizer token = new StringTokenizer( value, ",", false );
            while ( token.hasMoreTokens() )
            {
                String current = token.nextToken().trim();
                this.cmd.createArgument().setValue( current );
            }
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     * 
     * @see #addArgIfNotEmpty(String, String)
     * @see <a href="http://jakarta.apache.org/commons/lang/api/org/apache/commons/lang/SystemUtils.html#isJavaVersionAtLeast(float)">SystemUtils.html#isJavaVersionAtLeast(float)</a>
     * 
     * @param key
     *            the argument name.
     * @param value
     *            the argument value to be added.
     * @param requiredJavaVersion  
     *            the required Java version, for example 1.31f or 1.4f
     */
    private void addArgIfNotEmpty( final String key, final String value, final float requiredJavaVersion )
    {
        if ( SystemUtils.isJavaVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIfNotEmpty( key, value );
        }
        else
        {
            getLog().warn( key + " option is not supported on Java version < " + requiredJavaVersion );
        }
    }

    /**
     * Returns an input stream for reading the specified resource from the
     * current class loader.
     * 
     * @param resource
     *            the resource
     * @return InputStream 
     *            An input stream for reading the resource, or <tt>null</tt>
     *            if the resource could not be found
     * @throws Exception
     *             if any
     */
    private static InputStream getStream( final String resource )
        throws Exception
    {
        return JavadocReport.class.getClassLoader().getResourceAsStream( resource );
    }

    /**
     * Convenience method that copy the <code>STYLESHEET_NAME</code> from the
     * <code>RESOURCE_DIR</code> to a given outputDirectory.
     * 
     * @see #DEFAULT_STYLESHEET_LOCATION
     * @see #RESOURCE_DIR
     * 
     * @param outputDirectory
     *            the output directory
     * @throws Exception
     *             if any
     */
    private void copyDefaultStylesheet( final File outputDirectory )
        throws Exception
    {

        if ( ( outputDirectory == null ) || ( !outputDirectory.exists() ) )
        {
            throw new IOException( "The outputDirectory " + outputDirectory + " doesn't exists." );
        }

        InputStream is = getStream( DEFAULT_STYLESHEET_LOCATION );

        if ( is == null )
        {
            throw new IOException( "The resource " + DEFAULT_STYLESHEET_LOCATION + " doesn't exists." );
        }

        File outputFile = new File( outputDirectory, DEFAULT_STYLESHEET_LOCATION );

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