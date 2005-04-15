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

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.codehaus.surefire.SurefireBooter;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal test
 * @description Run tests using surefire
 * @parameter name="basedir"
 * type="String"
 * required="true"
 * validator="validator"
 * expression="#basedir"
 * description=""
 * @parameter name="classesDirectory"
 * type="String"
 * required="true"
 * validator="validator"
 * expression="#project.build.outputDirectory"
 * description=""
 * @parameter name="testClassesDirectory"
 * type="String"
 * required="true"
 * validator="validator"
 * expression="#project.build.testOutputDirectory"
 * description=""
 * @parameter name="includes"
 * type="java.util.List"
 * required="false"
 * validator=""
 * description=""
 * expression=""
 * @parameter name="excludes"
 * type="java.util.List"
 * required="false"
 * validator=""
 * description=""
 * expression=""
 * @parameter name="classpathElements"
 * type="java.util.List"
 * required="true"
 * validator=""
 * expression="#project.testClasspathElements"
 * description=""
 * @parameter name="reportsDirectory"
 * type="String"
 * required="false"
 * validator=""
 * expression="#project.build.directory/surefire-reports"
 * description="Base directory where all reports are written to."
 * @parameter name="test"
 * type="String"
 * required="false"
 * validator=""
 * expression="#test"
 * description="Specify this parameter if you want to use the test regex notation to select tests to run."
 * @parameter name="localRepository"
 * type="org.apache.maven.artifact.repository.ArtifactRepository"
 * required="true"
 * validator=""
 * expression="#localRepository"
 * description=""
 * @todo make version of junit and surefire configurable
 * @todo make report to be produced configurable
 */
public class SurefirePlugin
    extends AbstractPlugin
{
    private String basedir;

    private String classesDirectory;

    private String testClassesDirectory;

    private List classpathElements;

    private String reportsDirectory;

    private String test;

    private List includes;

    private List excludes;

    private ArtifactRepository localRepository;

    public void execute()
        throws PluginExecutionException
    {
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
                                       new Object[]{basedir, includes, excludes} );
        }
        else
        {
            // defaults here, qdox doesn't like the end javadoc value
            if ( includes == null )
            {
                includes = new ArrayList();
                includes.add( "**/*Test.java" );
            }
            if ( excludes == null )
            {
                excludes = new ArrayList();
                excludes.add( "**/Abstract*Test.java" );
            }

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
                                       new Object[]{basedir, includes, excludes} );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        System.setProperty( "basedir", basedir );

        // TODO: we should really just trust the plugin classloader?
        try
        {
            DefaultArtifact artifact = new DefaultArtifact( "junit", "junit", "3.8.1", "jar" );
            File file = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
            surefireBooter.addClassPathUrl( file.getAbsolutePath() );
            artifact = new DefaultArtifact( "surefire", "surefire", "1.2", "jar" );
            file = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
            surefireBooter.addClassPathUrl( file.getAbsolutePath() );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new PluginExecutionException( "Error finding surefire JAR", e );
        }

        surefireBooter.addClassPathUrl( new File( classesDirectory ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory ).getPath() );

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            surefireBooter.addClassPathUrl( (String) i.next() );
        }

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReporter" );

        surefireBooter.addReport( "org.codehaus.surefire.report.FileReporter" );

        boolean success = false;
        try
        {
            success = surefireBooter.run();
        }
        catch ( Exception e )
        {
            // TODO: better handling
            throw new PluginExecutionException( "Error executing surefire", e );
        }

        if ( !success )
        {
            throw new PluginExecutionException( "There are some test failures." );
        }
    }

    protected String[] split( String str, String separator, int max )
    {
        StringTokenizer tok = null;
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
        int lastTokenBegin = 0;
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
}
