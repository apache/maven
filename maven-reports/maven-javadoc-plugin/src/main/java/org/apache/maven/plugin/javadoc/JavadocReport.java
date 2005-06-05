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
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * @goal javadoc
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 */
public class JavadocReport
    extends AbstractMavenReport
{
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
        if ( getConfiguration() == null )
        {
            throw new MavenReportException( "You must specify a report configuration." );
        }

        executeReport( locale);
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        try
        {
            File outputDir = new File( getConfiguration().getReportOutputDirectory().getAbsolutePath() + "/apidocs" );
            outputDir.mkdirs();

            int actualYear = Calendar.getInstance().get( Calendar.YEAR );
            String year;
            if ( getConfiguration().getModel().getInceptionYear() != null
                 && Integer.valueOf( getConfiguration().getModel().getInceptionYear() ).intValue() == actualYear )
            {
                year = getConfiguration().getModel().getInceptionYear();
            }
            else
            {
                year = getConfiguration().getModel().getInceptionYear() + "-" + String.valueOf( actualYear );
            }

            StringBuffer classpath = new StringBuffer();
            for ( Iterator i = getConfiguration().getProject().getCompileClasspathElements().iterator(); i.hasNext(); )
            {
                classpath.append( (String) i.next() );
                if ( i.hasNext() )
                {
                    classpath.append( ";" );
                }
            }

            StringBuffer sourcePath = new StringBuffer();
            String[] fileList = new String[1];
            for ( Iterator i = getConfiguration().getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String sourceDirectory = (String) i.next();
                fileList = FileUtils.getFilesFromExtension( sourceDirectory, new String[] { "java" } );
                sourcePath.append( sourceDirectory );
            }

            File javadocDirectory = new File( getConfiguration().getProject().getBuild().getDirectory() + "/javadoc" );
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

            Commandline cl = new Commandline();
            cl.setWorkingDirectory( javadocDirectory.getAbsolutePath() );
            cl.setExecutable( getJavadocPath() );
            cl.createArgument().setValue( "-use" );
            cl.createArgument().setValue( "-version" );
            cl.createArgument().setValue( "-author" );
            cl.createArgument().setValue( "-windowtitle" );
            cl.createArgument().setValue(
                                          getConfiguration().getModel().getName() + " "
                                              + getConfiguration().getModel().getVersion() );
            cl.createArgument().setValue( "-bottom" );
            cl.createArgument().setValue( "Copyright &copy; " + year + " "
                                          + getConfiguration().getModel().getOrganization().getName()
                                          + ". All Rights Reserved." );
            cl.createArgument().setValue( "-sourcePath" );
            cl.createArgument().setValue( sourcePath.toString() );
            cl.createArgument().setValue( "-d" );
            cl.createArgument().setValue( outputDir.getAbsolutePath() );
            cl.createArgument().setValue( "-classpath" );
            cl.createArgument().setValue( classpath.toString() );
            cl.createArgument().setValue( "@files" );
            System.out.println( getJavadocPath() );
            System.out.println( Commandline.toString( cl.getCommandline() ) );
            System.out.println( cl.getWorkingDirectory() );
            int exitCode = CommandLineUtils.executeCommandLine( cl, new DefaultConsumer(), new DefaultConsumer() );
            if ( exitCode != 0 )
            {
                throw new MavenReportException( "exit code: " + exitCode );
            }
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "An error is occurred in javadoc report generation.", e );
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
     * Return path of javadoc tool.
     * 
     * @return path of javadoc tool
     */
    private String getJavadocPath()
    {
        // TODO: this could probably be improved/configured
        // TODO: doesn't work with spaces in java.home
        String fileSeparator = System.getProperty( "file.separator" );
        File f = new File( System.getProperty( "java.home" ), "../bin/javadoc" );
        return f.getAbsolutePath();
    }
}
