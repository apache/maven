package org.apache.maven.test;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.surefire.SurefireBooter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @requiresDependencyResolution test
 * @goal test
 * @phase test
 * @description Run tests using Surefire
 * @todo make version of junit and surefire configurable
 * @todo make report to be produced configurable
 */
public class SurefirePlugin
    extends AbstractMojo
{
    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by System.getProperty("basedir").
     *
     * @parameter expression="${basedir}"
     * @required
     */
    private File basedir;

    /**
     * The directory containing generated classes of the project being tested.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * The directory containing generated test classes of the project being tested.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    /**
     * The classpath elements of the project being tested.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private String reportsDirectory;

    /**
     * Specify this parameter if you want to use the test regex notation to select tests to run.
     *
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in testing.
     *
     * @parameter
     */
    private List includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in testing.
     *
     * @parameter
     */
    private List excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use System.setProperty( "localRepository").
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     */
    private Properties systemProperties;

    /**
     * List of of Plugin Artifacts.
     *
     * @parameter expression="${plugin.artifacts}"
     */
    private List pluginArtifacts;

    /**
     * Option to print summary of test suites or just print the test cases that has errors.
     *
     * @parameter expression="${surefire.printSummary}"
     * default-value="true"
     */
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated.  Can be set as brief, plain, or xml.
     *
     * @parameter expression="${surefire.reportFormat}"
     * default-value="brief"
     */
    private String reportFormat;

    /**
     * Option to generate a file test report or just output the test report to the console.
     *
     * @parameter expression="${surefire.useFile}"
     * default-value="true"
     */
    private boolean useFile;

    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Tests are skipped." );

            return;
        }

        if ( !testClassesDirectory.exists() )
        {
            getLog().info( "No tests to run." );

            return;
        }

        // ----------------------------------------------------------------------
        // Setup the surefire booter
        // ----------------------------------------------------------------------

        SurefireBooter surefireBooter = new SurefireBooter();

        getLog().info( "Setting reports dir: " + reportsDirectory );

        surefireBooter.setReportsDirectory( reportsDirectory );

        // ----------------------------------------------------------------------
        // Check to see if we are running a single test. The raw parameter will
        // come through if it has not been set.
        // ----------------------------------------------------------------------

        if ( test != null )
        {
            // FooTest -> **/FooTest.java

            List includes = new ArrayList();

            List excludes = new ArrayList();

            String[] testRegexes = split( test, ",", -1 );

            for ( int i = 0; i < testRegexes.length; i++ )
            {
                includes.add( "**/" + testRegexes[i] + ".java" );
            }

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
                                       new Object[]{testClassesDirectory, includes, excludes} );
        }
        else
        {
            // defaults here, qdox doesn't like the end javadoc value
            // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
            if ( includes == null || includes.size() == 0 )
            {
                includes = new ArrayList(
                    Arrays.asList( new String[]{"**/Test*.java", "**/*Test.java", "**/*TestCase.java"} ) );
            }
            if ( excludes == null || excludes.size() == 0 )
            {
                excludes = new ArrayList(
                    Arrays.asList( new String[]{"**/Abstract*Test.java", "**/Abstract*TestCase.java"} ) );
            }

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
                                       new Object[]{testClassesDirectory, includes, excludes} );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        System.setProperty( "basedir", basedir.getAbsolutePath() );

        System.setProperty( "localRepository", localRepository.getBasedir() );

        // Add all system properties configured by the user
        if ( systemProperties != null )
        {
            Enumeration propertyKeys = systemProperties.propertyNames();

            while ( propertyKeys.hasMoreElements() )
            {
                String key = (String) propertyKeys.nextElement();

                System.setProperty( key, systemProperties.getProperty( key ) );

                getLog().debug( "Setting system property [" + key + "]=[" + systemProperties.getProperty( key ) + "]" );
            }
        }

        getLog().debug( "Test Classpath :" );

        getLog().debug( testClassesDirectory.getPath() );

        surefireBooter.addClassPathUrl( testClassesDirectory.getPath() );

        getLog().debug( classesDirectory.getPath() );

        surefireBooter.addClassPathUrl( classesDirectory.getPath() );

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();

            getLog().debug( classpathElement );

            surefireBooter.addClassPathUrl( classpathElement );
        }

        for ( Iterator i = pluginArtifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // TODO: this is crude for now. We really want to get "surefire-booter" and all its dependencies, but the
            // artifacts don't keep track of their children. We could just throw all of them in, but that would add an
            // unnecessary maven-artifact dependency which is precisely the reason we are isolating the classloader
            if ( "junit".equals( artifact.getArtifactId() ) || "surefire".equals( artifact.getArtifactId() ) ||
                "surefire-booter".equals( artifact.getArtifactId() ) ||
                "plexus-utils".equals( artifact.getArtifactId() ) )
            {
                getLog().debug( "Adding to surefire test classpath: " + artifact.getFile().getAbsolutePath() );

                surefireBooter.addClassPathUrl( artifact.getFile().getAbsolutePath() );
            }
        }

        addReporters( surefireBooter );

        boolean success;

        try
        {
            success = surefireBooter.run();
        }
        catch ( Exception e )
        {
            // TODO: better handling
            throw new MojoExecutionException( "Error executing surefire", e );
        }

        if ( !success )
        {
            String msg = "There are some test failure.";

            if ( testFailureIgnore )
            {
                getLog().error( msg );
            }
            else
            {
                throw new MojoExecutionException( msg );
            }
        }
    }

    protected String[] split( String str, String separator, int max )
    {
        StringTokenizer tok;

        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();

        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        String[] list = new String[listSize];

        int i = 0;

        int lastTokenBegin;

        int lastTokenEnd = 0;

        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();

                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );

                list[i] = str.substring( lastTokenBegin );

                break;
            }
            else
            {
                list[i] = tok.nextToken();

                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );

                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }

        return list;
    }

    /**
     * <p> Adds Reporters that will generate reports with different formatting.
     * <p> The Reporter that will be added will be based on the value of the parameter
     * useFile, reportFormat, and printSummary.
     *
     * @param surefireBooter The surefire booter that will run tests.
     */
    private void addReporters( SurefireBooter surefireBooter )
    {

        if ( useFile )
        {
            if ( printSummary )
            {
                surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReporter" );
            }
            else
            {
                surefireBooter.addReport( "org.codehaus.surefire.report.SummaryConsoleReporter" );
            }

            if ( reportFormat.equals( "brief" ) )
            {
                surefireBooter.addReport( "org.codehaus.surefire.report.BriefFileReporter" );
            }
            else if ( reportFormat.equals( "plain" ) )
            {
                surefireBooter.addReport( "org.codehaus.surefire.report.FileReporter" );
            }
        }
        else
        {
            if ( reportFormat.equals( "brief" ) )
            {
                surefireBooter.addReport( "org.codehaus.surefire.report.BriefConsoleReporter" );
            }
            else if ( reportFormat.equals( "plain" ) )
            {
                surefireBooter.addReport( "org.codehaus.surefire.report.DetailedConsoleReporter" );
            }
        }
        surefireBooter.addReport( "org.codehaus.surefire.report.XMLReporter" );
    }
}
