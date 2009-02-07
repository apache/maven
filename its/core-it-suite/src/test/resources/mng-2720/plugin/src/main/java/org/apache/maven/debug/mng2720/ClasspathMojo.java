package org.apache.maven.debug.mng2720;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.Iterator;
import java.util.List;

/**
 * @goal test
 * @requiresDependencyResolution compile
 * @phase package
 */
public class ClasspathMojo
    implements Mojo
{
    
    /**
     * @parameter default-value="${project}"
     * @readonly
     */
    private MavenProject project;
    
    /**
     * @parameter default-value="${project.compileClasspathElements}"
     * @readonly
     */
    private List compileClasspathElements;

    private Log log;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( "child2".equals( project.getArtifactId() ) )
        {
            boolean found = false;
            for ( Iterator it = compileClasspathElements.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                if ( path.indexOf( "child1-1.jar" ) > -1 )
                {
                    found = true;
                    break;
                }
            }
            
            if ( !found )
            {
                throw new MojoExecutionException( "child1-1.jar dependency artifact path not found in compile classpath for project: " + project.getId() );
            }
        }
        else if ( "child3".equals( project.getArtifactId() ) )
        {
            boolean found = false;
            for ( Iterator it = compileClasspathElements.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                if ( path.indexOf( "child1-1-tests.jar" ) > -1 )
                {
                    found = true;
                    break;
                }
            }
            
            if ( !found )
            {
                throw new MojoExecutionException( "child1-1-tests.jar dependency artifact path not found in compile classpath for project: " + project.getId() );
            }
        }
        
        log.info( "Tests succeeded." );
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

}
