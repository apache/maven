package org.apache.maven.errors;

import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.errors.CoreErrorReporter;
import org.aspectj.lang.reflect.SourceLocation;

public privileged aspect CoreReporterDebugAspect
{

//    before( Throwable key, DefaultCoreErrorReporter reporter ):
//        call( void DefaultCoreErrorReporter.registerBuildError( Throwable, .. ) )
//        && args( key, .. )
//        && target( reporter )
//    {
//        SourceLocation location = thisJoinPoint.getSourceLocation();
//        System.out.println( "Registering: " + key + "\nfrom: " + location.getFileName() + ", line: " + location.getLine() + "\nreporter is: " + reporter );
//    }

}
