package org.apache.maven.test;

import org.codehaus.surefire.SurefireBooter;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;
import java.lang.reflect.Array;

/**
 * @goal test
 *
 * @description Run tests using surefire
 *
 * @prereq compiler:compile
 * @prereq compiler:testCompile
 * @prereq resources:resources
 * @prereq resources:testResources
 *
 * @parameter
 *  name="mavenRepoLocal"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#maven.repo.local"
 *  description=""
 * @parameter
 *  name="basedir"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#basedir"
 *  description=""
 * @parameter
 *  name="classesDirectory"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#project.build.output"
 *  description=""
 * @parameter
 *  name="testClassesDirectory"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#project.build.testOutput"
 *  description=""
 * @parameter
 *  name="includes"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.unitTest.includes"
 *  description=""
 * @parameter
 *  name="excludes"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.unitTest.excludes"
 *  description=""
 * @parameter
 *  name="classpathElements"
 *  type="String[]"
 *  required="true"
 *  validator=""
 *  expression="#project.classpathElements"
 *  description=""
 * @parameter
 *  name="reportsDirectory"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#project.build.directory/surefire-reports"
 *  description="Base directory where all reports are written to."
 * @parameter
 *  name="test"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#test"
 *  description="Specify this parameter if you want to use the test regex notation to select tests to run."
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 *
 * @todo make version of junit and surefire configurable
 * @todo make report to be produced configurable
 */
public class SurefirePlugin
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String mavenRepoLocal = (String) request.getParameter( "mavenRepoLocal" );

        String basedir = (String) request.getParameter( "basedir" );

        String classesDirectory = (String) request.getParameter( "classesDirectory" );

        String testClassesDirectory = (String) request.getParameter( "testClassesDirectory" );

        String[] classpathElements = (String[]) request.getParameter( "classpathElements" );

        String reportsDirectory = (String) request.getParameter( "reportsDirectory" );

        String test = (String) request.getParameter( "test" );

        // ----------------------------------------------------------------------
        // Setup the surefire booter
        // ----------------------------------------------------------------------

        SurefireBooter surefireBooter = new SurefireBooter();

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

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{basedir, includes, excludes} );
        }
        else
        {
            List includes = (List) request.getParameter( "includes" );

            List excludes = (List) request.getParameter( "excludes" );

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{basedir, includes, excludes} );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        System.setProperty( "basedir", basedir );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.2-SNAPSHOT.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( classesDirectory ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory ).getPath() );

        for ( int i = 0; i < classpathElements.length; i++ )
        {
            if(classpathElements[i] != null)
            {
                surefireBooter.addClassPathUrl( classpathElements[i] );
            }
        }

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReporter" );

        surefireBooter.addReport( "org.codehaus.surefire.report.FileReporter" );

        boolean success = surefireBooter.run();

        if ( !success )
        {
            response.setExecutionFailure( true, new SurefireFailureResponse( null ) );
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
