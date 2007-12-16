package org.apache.maven;

import org.apache.maven.execution.MavenExecutionRequest;

import java.util.List;

public aspect CoreDebuggingAspect
{

//    after( MavenExecutionRequest request ) returning( List projects ):
//        call( List DefaultMaven.getProjects( MavenExecutionRequest ) )
//        && args( request )
//    {
//        System.out.println( "Got projects-list of size " + ( projects == null ? "null" : "" + projects.size() ) + ":\n\n" + projects );
//    }

}
