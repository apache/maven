package org.apache.maven.its.mng7349;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

@Mojo( name = "echoMojo", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true )
public class EchoMojo extends AbstractMojo
{
    @Parameter( defaultValue = "World!" )
    String helloString;

    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().warn( "=====================================================================================" );
        getLog().warn( "Hello " + helloString );
        getLog().warn( "=====================================================================================" );
    }
}
