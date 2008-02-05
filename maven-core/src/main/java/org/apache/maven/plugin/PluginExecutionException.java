package org.apache.maven.plugin;

import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;

public class PluginExecutionException
    extends PluginManagerException
{

    private final MojoExecution mojoExecution;

    public PluginExecutionException( MojoExecution mojoExecution,
                                     MavenProject project,
                                     String message )
    {
        super( mojoExecution.getMojoDescriptor(), project, message );
        this.mojoExecution = mojoExecution;
    }

    public PluginExecutionException( MojoExecution mojoExecution,
                                     MavenProject project,
                                     MojoExecutionException cause )
    {
        super( mojoExecution.getMojoDescriptor(), project, "Mojo execution failed.", cause );
        this.mojoExecution = mojoExecution;
    }

    public PluginExecutionException( MojoExecution mojoExecution,
                                     MavenProject project,
                                     DuplicateArtifactAttachmentException cause )
    {
        super( mojoExecution.getMojoDescriptor(), project, "Mojo execution failed.", cause );
        this.mojoExecution = mojoExecution;
    }

    public MojoExecution getMojoExecution()
    {
        return mojoExecution;
    }

}
