package org.apache.maven.plugin;

import org.apache.maven.execution.MavenSession;

import java.util.Collection;

public interface PluginContext {

    Collection<MojoExecution> getMojoExecutionsForGoal(String goal) throws Exception;

    Object getMojoParameterFor(MojoExecution mojoExecution, String xPath) throws Exception;

    void executeMojo(MojoExecution mojoExecution, MavenSession session) throws Exception;
}
