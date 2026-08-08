package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author jdcasey
 */
@org.apache.maven.plugins.annotations.Mojo(name = "test")
public class TestPlugin
        implements Mojo
{

    private Log log;

    @Component
    private MavenProjectHelper mavenProjectHelper;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        mavenProjectHelper.attachArtifact( project, "pom", "classifier", project.getFile() );
        mavenProjectHelper.attachArtifact( project, "pom", "classifier", project.getFile() );
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
