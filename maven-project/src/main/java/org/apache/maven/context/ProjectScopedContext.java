package org.apache.maven.context;

import org.apache.maven.project.MavenProject;

public class ProjectScopedContext
    extends ScopedBuildContext
{
    
    public ProjectScopedContext( MavenProject project, BuildContext parentBuildContext )
    {
        super( project.getId(), parentBuildContext );
    }

}
