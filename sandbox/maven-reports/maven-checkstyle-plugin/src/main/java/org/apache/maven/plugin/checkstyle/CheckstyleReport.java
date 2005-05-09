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

import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageNamesLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.XMLLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 */
public class CheckstyleReport
    extends AbstractMavenReport
{
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

    private URL configFile = getClass().getResource( "/config/sun_checks.xml" );

    private String extraFormatter = "plain";

    private String resultFileName = "checkstyle-result.txt";

    private String packageNamesFile;

    private boolean failedOnError = false;

    public String getName()
    {
        return "Checkstyle";
    }

    public String getDescription()
    {
        return "Report on coding style conventions.";
    }

    public void execute()
        throws MavenReportException
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        AuditListener listener = null;

        if ( StringUtils.isNotEmpty( extraFormatter ) )
        {
            FileOutputStream out;
            File resultFile = new File( getConfiguration().getOutputDirectory(), resultFileName );
            try
            {
                File parentFile = resultFile.getParentFile();
                parentFile.mkdirs();
                out = new FileOutputStream( resultFile );
            }
            catch( IOException e )
            {
                throw new MavenReportException( "Can't access to " + resultFile.getAbsolutePath(), e );
            }

            if ( "xml".equals( extraFormatter ) )
            {
                listener = new XMLLogger( out, true );
            }
            else if ( "plain".equals( extraFormatter ) )
            {
                listener = new DefaultLogger( out, true );
            }
            else
            {
                throw new MavenReportException( "Invalid format: (" + extraFormatter + "). Must be 'plain' or 'xml'." );
            }
        }

        File[] files;
        try
        {
            List filesList = getFilesToProcess( "**/*.java", null );
            files = new File[filesList.size()];
            int i = 0;
            for ( Iterator iter = filesList.iterator(); iter.hasNext(); )
            {
                files[i++] = (File) iter.next();
            }
        }
        catch( IOException e )
        {
            throw new MavenReportException( "Can't parse " + getConfiguration().getSourceDirectory(), e );
        }

        Configuration config;

        try
        {
            config = ConfigurationLoader.loadConfiguration( configFile.toString(),
                                                            new PropertiesExpander( createOverridingProperties() ) );
        }
        catch ( CheckstyleException e )
        {
            throw new MavenReportException( "Error loading config file : " + configFile.toString(), e );
        }

        ModuleFactory moduleFactory = null;

        if ( StringUtils.isNotEmpty( packageNamesFile ) )
        {
            try
            {
                moduleFactory = PackageNamesLoader.loadModuleFactory( packageNamesFile );
            }
            catch ( CheckstyleException e )
            {
                throw new MavenReportException( "Error loading package names file : " + packageNamesFile, e );
            }
        }

        Checker checker = null;

        try
        {
            checker = new Checker();

            checker.setModuleFactory( moduleFactory );

            checker.configure( config );

            AuditListener sinkListener = new CheckstyleReportListener( getSink(), getConfiguration().getSourceDirectory() );

            if ( listener != null )
            {
                checker.addListener( listener );
            }

            checker.addListener( sinkListener );
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "Unable to create Checker: " + e.getMessage(), e );
        }

        int nbErrors = checker.process( files );

        if ( checker != null )
        {
            checker.destroy();
        }
        
        if ( failedOnError && nbErrors > 0 )
        {
            throw new MavenReportException( "There are " + nbErrors + " formatting errors." );
        }
    }

    public String getOutputName()
    {
        return "checkstyle";
    }

    private List getFilesToProcess( String includes, String excludes )
        throws IOException
    {
        StringBuffer excludesStr = new StringBuffer();
        if ( StringUtils.isNotEmpty( excludes ) )
        {
            excludesStr.append(excludes);
        }
        for ( int i = 0; i < DEFAULT_EXCLUDES.length; i++ )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }
            excludesStr.append( DEFAULT_EXCLUDES[i] );
        }

        return FileUtils.getFiles( new File( getConfiguration().getSourceDirectory() ), includes, excludesStr.toString() );
    }

    private Properties createOverridingProperties()
    {
        Properties props = new Properties();
        props.setProperty( "checkstyle.header.file", "LICENSE.txt" );
        props.setProperty( "checkstyle.cache.file", "target/checkstyle-cachefile" );
        return props;
    }
}