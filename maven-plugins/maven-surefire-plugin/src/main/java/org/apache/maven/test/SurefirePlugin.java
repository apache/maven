package org.apache.maven.test;

import org.codehaus.surefire.SurefireBooter;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.util.List;

/**
 * @maven.plugin.id surefire
 * @maven.plugin.description A maven2 plugin which uses surefire as a test runner
 *
 * @parameter mavenRepoLocal String required validator
 * @parameter basedir String required validator
 * @parameter includes String required validator
 * @parameter excludes String required validator
 * @parameter classpathElements String[] required validator
 *
 * @goal.name test
 * @goal.test.description Run tests using surefire
 *
 * @goal.test.prereq test:compile
 * @goal.test.prereq resources
 * @goal.test.prereq test:resources
 *
 * @goal.test.parameter mavenRepoLocal #maven.repo.local
 * @goal.test.parameter basedir #basedir
 * @goal.test.parameter includes #project.build.unitTest.includes
 * @goal.test.parameter excludes #project.build.unitTest.excludes
 * @goal.test.parameter classpathElements #project.classpathElements

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

        List includes = (List) request.getParameter( "includes" );

        List excludes = (List) request.getParameter( "excludes" );

        String[] classpathElements = (String[]) request.getParameter( "classpathElements" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        System.setProperty( "basedir", basedir );

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{basedir, includes, excludes} );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.0.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( basedir, "target/classes" ).getPath() );

        surefireBooter.addClassPathUrl( new File( basedir, "target/test-classes" ).getPath() );

        for ( int i = 0; i < classpathElements.length; i++ )
        {
            surefireBooter.addClassPathUrl( classpathElements[i] );
        }

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReport" );

        surefireBooter.run();
    }
}
