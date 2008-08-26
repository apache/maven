package org.apache.maven;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;

public interface ConfigurationInterpolator
{
    
    Object interpolate( Object configObject, MavenProject project, ProjectBuilderConfiguration config )
        throws ConfigurationInterpolationException;

}
