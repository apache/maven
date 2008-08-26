package org.apache.maven;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.StringSearchModelInterpolator;

public class ReflectionConfigurationInterpolator
    extends StringSearchModelInterpolator
    implements ConfigurationInterpolator
{

    public Object interpolate( Object configObject, MavenProject project, ProjectBuilderConfiguration config )
        throws ConfigurationInterpolationException
    {
        try
        {
            interpolateObject( configObject, project.getModel(), project.getBasedir(), config, getLogger().isDebugEnabled() );
        }
        catch ( ModelInterpolationException e )
        {
            throw new ConfigurationInterpolationException( "Error interpolating configuration for project: " + project.getId() + "\n\n" + configObject, e );
        }
        
        return configObject;
    }

}
