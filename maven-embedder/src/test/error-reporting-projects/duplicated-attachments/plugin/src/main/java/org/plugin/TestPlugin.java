package org.plugin;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.MavenProject;

/**
 * @goal test
 *
 * @author jdcasey
 */
public class TestPlugin
    implements Mojo
{

    private Log log;

    /**
     * @component
     */
    private MavenProjectHelper mavenProjectHelper;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
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
