package org.apache.maven.lifecycle;

import java.util.List;

import org.apache.maven.plugin.MojoExecution;

//TODO: lifecycles being executed
//TODO: what runs in each phase
//TODO: plugins that need downloading
//TODO: project dependencies that need downloading
//TODO: unfortunately the plugins need to be downloaded in order to get the plugin.xml file. need to externalize this from the plugin archive.
//TODO: this will be the class that people get in IDEs to modify
public class MavenExecutionPlan
{
    /** Individual executions that must be performed. */
    private List<MojoExecution> executions;
    
    /** For project dependency resolution, the scope of resolution required if any. */
    private String requiredDependencyResolutionScope;

    public MavenExecutionPlan( List<MojoExecution> executions, String requiredDependencyResolutionScope )
    {
        this.executions = executions;
        this.requiredDependencyResolutionScope = requiredDependencyResolutionScope;
    }

    public List<MojoExecution> getExecutions()
    {
        return executions;
    }

    public String getRequiredResolutionScope()
    {
        return requiredDependencyResolutionScope;
    }        
}
