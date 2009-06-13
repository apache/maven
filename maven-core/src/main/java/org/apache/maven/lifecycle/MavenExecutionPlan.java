package org.apache.maven.lifecycle;

import java.util.List;
import java.util.Set;

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
    
    /** For project dependency resolution, the scopes of resolution required if any. */
    private Set<String> requiredDependencyResolutionScopes;

    public MavenExecutionPlan( List<MojoExecution> executions, Set<String> requiredDependencyResolutionScopes )
    {
        this.executions = executions;
        this.requiredDependencyResolutionScopes = requiredDependencyResolutionScopes;
    }

    public List<MojoExecution> getExecutions()
    {
        return executions;
    }

    public Set<String> getRequiredResolutionScopes()
    {
        return requiredDependencyResolutionScopes;
    }        
}
