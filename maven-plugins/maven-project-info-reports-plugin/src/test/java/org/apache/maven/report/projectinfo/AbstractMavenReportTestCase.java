package org.apache.maven.report.projectinfo;

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

import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * An abstract TestCase class to test <code>Maven Reports</code> generated.
 *
 * @phase test
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public abstract class AbstractMavenReportTestCase
    extends PlexusTestCase
{
    /**
     * The default projects directory.
     */
    private static String PROJECTS_DIR = "src/test/projects";

    /**
     * The default M2 goal to generate reports.
     */
    protected static String M2_SITE_GOAL = "site:site";

    /**
     * Set this to 'true' to bypass unit tests entirely.
     *
     * @parameter expression="${maven.test.skip}"
     */
    protected boolean skip;

    /**
     * The default locale is English.
     */
    protected Locale locale = Locale.ENGLISH;

    /**
     * The current project to be test.
     */
    protected MavenProject testMavenProject;

    /**
     * The I18N plexus component.
     */
    private I18N i18n;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        i18n = (I18N) lookup( I18N.ROLE );
    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#getCustomConfiguration()
     */
    protected InputStream getCustomConfiguration()
        throws Exception
    {
        // Allow sub classes to have their own configuration...
        if ( super.getConfiguration() == null )
        {
            String className = AbstractMavenReportTestCase.class.getName();

            String config = className.substring( className.lastIndexOf( "." ) + 1 ) + ".xml";

            return AbstractMavenReportTestCase.class.getResourceAsStream( config );
        }

        return null;
    }

    /**
     * Get the current locale
     *
     * @return the locale
     */
    protected Locale getLocale()
    {
        return locale;
    }

    /**
     * Get the current i18n
     *
     * @return the i18n
     */
    public I18N getI18n()
    {
        return i18n;
    }

    /**
     * Get the current Maven project
     *
     * @return the maven project
     */
    protected MavenProject getTestMavenProject()
    {
        return testMavenProject;
    }

    /**
     * Load and build a Maven project from the test projects directory.
     *
     * @see #getTestProjectDir()
     *
     * @param projectDirName not null name of the test project dir in the <code>PROJECTS_DIR</code> directory.
     * @throws Exception is any
     */
    protected void loadTestMavenProject( String projectDirName )
        throws Exception
    {
        File projectDir = getTestProjectDir( projectDirName );

        File pom = new File( projectDir, "pom.xml" );
        if ( !pom.exists() )
        {
            throw new IllegalArgumentException( "No 'pom.xml' file exists in the test project directory '"
                + projectDir.getAbsolutePath() + "'" );
        }

        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        ProfileManager profileManager = new DefaultProfileManager( getContainer() );

        testMavenProject = builder.build( pom, null, profileManager );
    }

    /**
     * Execute a m2 command line to execute the specific goal <code>M2_SITE_GOAL</code> to generate report
     * for the current Maven proeject.
     *
     * @see #M2_SITE_GOAL
     *
     * @throws CommandLineException if any Exception is caught
     */
    protected void executeMaven2CommandLine()
        throws CommandLineException
    {
        File workingDir = getTestProjectDir();

        Commandline cmd = createMaven2CommandLine( workingDir, M2_SITE_GOAL );

        int exitCode = CommandLineUtils.executeCommandLine( cmd, new DefaultConsumer(), new DefaultConsumer() );

        if ( exitCode != 0 )
        {
            throw new CommandLineException( "The command line failed. Exit code: " + exitCode );
        }
    }

    /**
     * Gets a trimmed String for the given key from the resource bundle defined by Plexus.
     *
     * @param key the key for the desired string
     * @return the string for the given key
     * @throws IllegalArgumentException if the parameter is empty.
     */
    protected String getString( String key )
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new IllegalArgumentException( "The key cannot be empty" );
        }

        return i18n.getString( key, getLocale() ).trim();
    }

    /**
     * Get the basedir for the current test Maven project.
     *
     * @see #getTestMavenProject()
     *
     * @return the basedir of the current test project
     */
    protected File getTestProjectDir()
    {
        return getTestMavenProject().getBasedir();
    }

    /**
     * Get the generated report as file in the test maven project.
     *
     * @see #getReportName()
     *
     * @return the generated report as file
     * @throws IOException if the return file doesnt exist
     */
    protected File getGeneratedReport()
        throws IOException
    {
        // TODO how to be more dynamic?
        String outputDirectory = getTestMavenProject().getBuild().getDirectory() + File.separator + "site";

        if ( getReportName() == null )
        {
            throw new IOException( "getReportName() should be return a report name." );
        }

        File report = new File( outputDirectory, getReportName() );
        if ( !report.exists() )
        {
            throw new IOException( "File not found. Attempted :" + report );
        }

        return report;
    }

    /**
     * Abstract method to get the report name to be tested, eg <code>index.html</code>
     *
     * @return the report name
     */
    protected abstract String getReportName();

    /**
     * Convenience method to create a m2 command line from a given working directory.
     * <p>We suppose that the <code>m2</code> executable is present in the command path</p>.
     *
     * @param workingDir a not null working directory.
     * @param goal the wanted goal
     * @return the m2 command line, eg <code>m2 clean:clean site:site</code>
     * @throws IllegalArgumentException if the parameter workingDir is empty or doesnt exist.
     */
    private static Commandline createMaven2CommandLine( File workingDir, String goal )
    {
        if ( workingDir == null )
        {
            throw new IllegalArgumentException( "The workingDir cant be null" );
        }
        if ( !workingDir.exists() )
        {
            throw new IllegalArgumentException( "The workingDir doesnt exist" );
        }

        Commandline cmd = new Commandline();

        cmd.setWorkingDirectory( workingDir.getAbsolutePath() );

        cmd.setExecutable( "m2" );
        cmd.createArgument().setValue( "clean:clean" );
        if ( !StringUtils.isEmpty( goal ) )
        {
            cmd.createArgument().setValue( goal );
        }

        return cmd;
    }

    /**
     * Get the path for the directory which contains test projects.
     *
     * @see #PROJECTS_DIR
     * @see PlexusTestCase#getBasedir()
     *
     * @return the projects directory full path.
     */
    private static String getTestProjectsPath()
    {
        return getBasedir() + File.separator + PROJECTS_DIR;
    }

    /**
     * Get a specific project path defined by the project name in the <code>PROJECTS_DIR</code> directory.
     *
     * @see #getTestProjectsPath()
     *
     * @param projectName not null name of the test project dir in the <code>PROJECTS_DIR</code> directory.
     * @return the specific path for a project in the test projects directory.
     * @throws IllegalArgumentException if the parameter is empty.
     */
    private static String getTestProjectPath( String projectName )
    {
        return getTestProjectsPath() + File.separator + projectName;
    }

    /**
     * Get the specific project file defined by the project name in the <code>PROJECTS_DIR</code> directory.
     *
     * @see #getTestProjectPath(String)
     *
     * @param projectName not null name of the test project dir in the <code>PROJECTS_DIR</code> directory.
     * @return the specific path for a project in the test projects directory.
     * @throws IOException if the return file doesnt exist
     * @throws IllegalArgumentException if the parameter is empty.
     */
    private static File getTestProjectDir( String projectName )
        throws IOException
    {
        File projectDir = new File( getTestProjectPath( projectName ) );
        if ( !projectDir.exists() )
        {
            throw new IOException( "File not found. Attempted :" + projectDir );
        }

        return projectDir;
    }
}
