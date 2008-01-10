package org.apache.maven.project.aspect;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;

public privileged aspect ProjectDebugAspect
{

//    before( String dir, MavenProject project ):
//        cflow( execution( * DefaultMavenProjectBuilder.buildInternal( .. ) ) )
//        && call( void MavenProject.addScriptSourceRoot( String ) )
//        && args( dir )
//        && target( project )
//    {
//        System.out.println( "Setting script-source-root from POM to: " + dir + " in project: " + project.getId() );
//    }
//
//    after( MavenProject project ) returning( List scriptSourceRoots ):
//        execution( List MavenProject.getScriptSourceRoots() )
//        && this( project )
//    {
//        System.out.println( "Using script-source-roots:\n\n" + StringUtils.join( scriptSourceRoots.iterator(), "\nfrom project: " + project.getId() ) );
//    }

}
