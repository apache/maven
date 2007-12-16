package org.apache.maven.project.aspect;

import org.apache.maven.project.error.ProjectErrorReporter;
import org.apache.maven.project.error.DefaultProjectErrorReporter;
import org.aspectj.lang.reflect.SourceLocation;

public privileged aspect ProjectReporterDebugAspect
{

//    before( Throwable key, String message, DefaultProjectErrorReporter reporter ):
//        call( void DefaultProjectErrorReporter.registerBuildError( Throwable, String, .. ) )
//        && args( key, message, .. )
//        && target( reporter )
//    {
//        SourceLocation location = thisJoinPoint.getSourceLocation();
//        System.out.println( "Registering: " + key + "\nfrom: " + location.getFileName() + ", line: " + location.getLine() + "\nreporter is: " + reporter + "\n\nMessage:\n\n" + message );
//    }
//
//    before():
//        execution( void DefaultProjectErrorReporter.clearErrors() )
//    {
//        System.out.println( "WARNING: CLEARING ALL ERROR REPORTS." );
//    }
//
//    after() returning( Throwable key ):
//        execution( Throwable ProjectErrorReporter+.findReportedException( Throwable ) )
//    {
//        if ( key != null )
//        {
//            System.out.println( "Found reported exception: " + key );
//        }
//    }

}
