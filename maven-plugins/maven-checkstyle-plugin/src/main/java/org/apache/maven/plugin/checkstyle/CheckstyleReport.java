package org.apache.maven.plugin.checkstyle;

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

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageNamesLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.FilterSet;
import com.puppycrawl.tools.checkstyle.filters.SuppressionsLoader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 * @goal checkstyle
 */
public class CheckstyleReport
    extends AbstractMavenReport
{
    /**
     * Specifies the directory where the report will be generated
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Specifies the names filter of the source files to be used for checkstyle
     *
     * @parameter default-value="**\/*.java"
     * @required
     */
    private String includes;

    /**
     * Specifies the names filter of the source files to be excluded for checkstyle
     *
     * @parameter
     */
    private String excludes;

    /**
     * Specifies what predefined check set to use. Available sets are
     * "sun" (for the Sun coding conventions), "turbine", and "avalon".
     * Default is sun.
     *
     * @parameter default-value="sun"
     */
    private String format;

    /**
     * Specifies the location of the checkstyle properties that will be used to check the source.
     *
     * @parameter
     */
    private File propertiesFile;

    /**
     * Specifies the URL of the checkstyle properties that will be used to check the source.
     *
     * @parameter
     */
    private URL propertiesURL;

    /**
     * Specifies the location of the License file (a.k.a. the header file) that is used by Checkstyle
     * to verify that source code has the correct copyright.
     *
     * @parameter expression="${basedir}/LICENSE.txt"
     */
    private File headerFile;

    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     *
     * @parameter expression="${project.build.directory}/checkstyle-cachefile"
     */
    private String cacheFile;

    /**
     * If null, the checkstyle task will display violations on stdout. Otherwise, the text file will be
     * created with the violations.
     *
     * @parameter
     */
    private File useFile;

    /**
     * Specifies the location of the supperssions XML file to use. The plugin defines a Checkstyle
     * property named <code>checkstyle.suppressions.file</code> with the value of this
     * property. This allows using the Checkstyle property your own custom checkstyle
     * configuration file when specifying a suppressions file.
     *
     * @parameter
     */
    private String suppressionsFile;

    /**
     * Specifies the path and filename to save the checkstyle output.  The format of the output file is
     * determined by the <code>outputFileFormat</code>
     *
     * @parameter expression="${project.build.directory}/checkstyle-result.txt"
     */
    private String outputFile;

    /**
     * Specifies the format of the output to be used when writing to the output file. Valid values are
     * "plain" and "xml"
     *
     * @parameter default-value="plain"
     */
    private String outputFileFormat;

    /**
     * Specifies the location of the package names XML to be used to configure Checkstyle
     *
     * @parameter
     */
    private String packageNamesFile;

    /**
     * Specifies if the build should fail upon a violation.
     *
     * @parameter default-value="false"
     */
    private boolean failsOnError;

    /**
     * Specifies the location of the source files to be used for Checkstyle
     *
     * @parameter expression="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    private StringOutputStream stringOutputStream;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.checkstyle.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.checkstyle.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
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
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !canGenerateReport() )
        {
            // TODO: failure if not a report
            throw new MavenReportException( "No source directory to process for checkstyle" );
        }

        Map files = executeCheckstyle();

        CheckstyleReportGenerator generator = new CheckstyleReportGenerator( getSink(), getBundle( locale ) );

        generator.generateReport( files );
    }

    private Map executeCheckstyle()
        throws MavenReportException
    {
        File[] files = new File[0];
        try
        {
            files = getFilesToProcess( includes, excludes );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Error getting files to process", e );
        }

        String configFile = getConfigFile();

        Properties overridingProperties = getOverridingProperties();

        Checker checker;

        try
        {
            ModuleFactory moduleFactory = getModuleFactory();

            FilterSet filterSet = getSuppressions();

            Configuration config =
                ConfigurationLoader.loadConfiguration( configFile, new PropertiesExpander( overridingProperties ) );

            checker = new Checker();

            if ( moduleFactory != null )
            {
                checker.setModuleFactory( moduleFactory );
            }

            if ( filterSet != null )
            {
                checker.addFilter( filterSet );
            }

            checker.configure( config );
        }
        catch ( CheckstyleException ce )
        {
            throw new MavenReportException( "Failed during checkstyle configuration", ce );
        }

        AuditListener listener = getListener();

        if ( listener != null )
        {
            checker.addListener( listener );
        }

        checker.addListener( getConsoleListener() );

        CheckstyleReportListener sinkListener = new CheckstyleReportListener( sourceDirectory );

        checker.addListener( sinkListener );

        int nbErrors = checker.process( files );

        checker.destroy();

        if ( stringOutputStream != null )
        {
            getLog().info( stringOutputStream.toString() );
        }

        if ( failsOnError && nbErrors > 0 )
        {
            // TODO: should be a failure, not an error. Report is not meant to throw an exception here (so site would
            // work regardless of config), but should record this information
            throw new MavenReportException( "There are " + nbErrors + " formatting errors." );
        }

        return sinkListener.getFiles();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "checkstyle";
    }

    private AuditListener getListener()
        throws MavenReportException
    {
        AuditListener listener = null;

        if ( StringUtils.isNotEmpty( outputFileFormat ) )
        {
            File resultFile = new File( outputFile );

            OutputStream out = getOutputStream( resultFile );

            if ( "xml".equals( outputFileFormat ) )
            {
                listener = new XMLLogger( out, true );
            }
            else if ( "plain".equals( outputFileFormat ) )
            {
                listener = new DefaultLogger( out, true );
            }
            else
            {
                // TODO: failure if not a report
                throw new MavenReportException(
                    "Invalid output file format: (" + outputFileFormat + "). Must be 'plain' or 'xml'." );
            }
        }

        return listener;
    }

    private OutputStream getOutputStream( File file )
        throws MavenReportException
    {
        File parentFile = file.getAbsoluteFile().getParentFile();

        if ( !parentFile.exists() )
        {
            parentFile.mkdirs();
        }

        FileOutputStream fileOutputStream = null;
        try
        {
            fileOutputStream = new FileOutputStream( file );
        }
        catch ( FileNotFoundException e )
        {
            throw new MavenReportException( "Unable to create output stream: " + file, e );
        }
        return fileOutputStream;
    }

    private File[] getFilesToProcess( String includes, String excludes )
        throws MavenReportException, IOException
    {
        StringBuffer excludesStr = new StringBuffer();

        if ( StringUtils.isNotEmpty( excludes ) )
        {
            excludesStr.append( excludes );
        }

        String[] defaultExcludes = FileUtils.getDefaultExcludes();
        for ( int i = 0; i < defaultExcludes.length; i++ )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }

            excludesStr.append( defaultExcludes[i] );
        }

        List files = FileUtils.getFiles( sourceDirectory, includes, excludesStr.toString() );

        return (File[]) files.toArray( EMPTY_FILE_ARRAY );
    }

    private Properties getOverridingProperties()
        throws MavenReportException
    {
        Properties p = new Properties();

        try
        {
            if ( propertiesFile != null )
            {
                if ( propertiesFile.exists() )
                {
                    p.load( new FileInputStream( propertiesFile ) );
                }
                else
                {
                    getLog().warn( "File '" + propertiesFile + "' not found - skipping" );
                }
            }
            else if ( propertiesURL != null )
            {
                p.load( propertiesURL.openStream() );
            }

            if ( headerFile != null )
            {
                p.setProperty( "checkstyle.header.file", headerFile.getAbsolutePath() );
            }

            if ( cacheFile != null )
            {
                p.setProperty( "checkstyle.cache.file", cacheFile );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Failed to get overriding properties", e );
        }

        return p;
    }

    private String getConfigFile()
        throws MavenReportException
    {
        URL configFile;

        if ( StringUtils.isEmpty( format ) || "sun".equalsIgnoreCase( format.trim() ) )
        {
            // By default
            configFile = getClass().getResource( "/config/sun_checks.xml" );
        }
        else if ( "turbine".equalsIgnoreCase( format.trim() ) )
        {
            configFile = getClass().getResource( "/config/turbine_checks.xml" );
        }
        else if ( "avalon".equalsIgnoreCase( format.trim() ) )
        {
            configFile = getClass().getResource( "/config/avalon_checks.xml" );
        }
        else
        {
            // TODO: failure if not a report
            throw new MavenReportException( "Invalid configuration file format: " + format );
        }

        return configFile.toString();
    }

    private ModuleFactory getModuleFactory()
        throws CheckstyleException
    {
        if ( StringUtils.isEmpty( packageNamesFile ) )
        {
            return null;
        }

        return PackageNamesLoader.loadModuleFactory( packageNamesFile );
    }

    private FilterSet getSuppressions()
        throws MavenReportException
    {
        if ( StringUtils.isEmpty( suppressionsFile ) )
        {
            return null;
        }

        try
        {
            return SuppressionsLoader.loadSuppressions( suppressionsFile );
        }
        catch ( CheckstyleException ce )
        {
            throw new MavenReportException( "failed to load suppressions XML: " + suppressionsFile, ce );
        }
    }

    private DefaultLogger getConsoleListener()
        throws MavenReportException
    {
        DefaultLogger consoleListener;

        if ( useFile == null )
        {
            stringOutputStream = new StringOutputStream();
            consoleListener = new DefaultLogger( stringOutputStream, false );
        }
        else
        {
            OutputStream out = getOutputStream( useFile );

            consoleListener = new DefaultLogger( out, true );
        }

        return consoleListener;
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "checkstyle-report", locale, CheckstyleReport.class.getClassLoader() );
    }

    public boolean canGenerateReport()
    {
        // TODO: would be good to scan the files here
        return super.canGenerateReport() && sourceDirectory.exists();
    }
}
