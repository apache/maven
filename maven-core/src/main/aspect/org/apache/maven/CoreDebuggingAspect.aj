package org.apache.maven;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.aspectj.lang.JoinPoint;

import java.util.Iterator;
import java.util.List;

public aspect CoreDebuggingAspect
{

//    after() throwing ( RuntimeException e ):
////        adviceexecution( )
////        && args( jp )
//        call( * *..*.*(..))
//        && !within( CoreDebuggingAspect+ )
//        && !handler( * )
//    {
//        System.out.println( "Error: " + e.getClass().getName() + "\nwas in join point: " + thisJoinPoint.toLongString() + "\n(at: " + thisJoinPoint.getSourceLocation() + ")" );
//    }

//    after( MavenExecutionRequest request ) returning( List projects ):
//        call( List DefaultMaven.getProjects( MavenExecutionRequest ) )
//        && args( request )
//    {
//        System.out.println( "Got projects-list of size " + ( projects == null ? "null" : "" + projects.size() ) + ":\n\n" + projects );
//    }

//    private ClassRealm pluginRealm;
//
//    after() returning( ClassRealm realm ):
//        call( ClassRealm PluginDescriptor.getClassRealm() )
//        && cflow( execution( * DefaultPluginManager.executeMojo( .. ) ) )
//    {
//        pluginRealm = realm;
//    }
//
//    after():
//        execution( * DefaultPluginManager.executeMojo( .. ) )
//    {
//        pluginRealm = null;
//    }
//
//    void around():
//        call( void Mojo+.execute( .. ) )
//    {
//        try
//        {
//            proceed();
//        }
//        catch( Error err )
//        {
//            System.out.println( "Plugin realm was " + pluginRealm + ":\n\n\n" );
//            pluginRealm.display();
//
//            throw err;
//        }
//    }
//
//    after() returning( List reports ):
//        cflow( execution( * PluginParameterExpressionEvaluator.evaluate( .. ) ) )
//        && call( List MavenSession.getReports() )
//    {
//        System.out.println( "Injecting reports for ${reports} expression.\n\n" );
//        if ( reports != null && !reports.isEmpty() )
//        {
//            for ( Iterator it = reports.iterator(); it.hasNext(); )
//            {
//                Object report = it.next();
//                System.out.println( "Report: " + report + " has classloader:\n" + report.getClass().getClassLoader() );
//            }
//        }
//        System.out.println( "\n\n" );
//    }

}
