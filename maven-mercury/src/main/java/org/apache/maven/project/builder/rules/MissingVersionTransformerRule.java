package org.apache.maven.project.builder.rules;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.legacy.PomTransformer;
import org.apache.maven.project.builder.legacy.TransformerRule;

import java.util.List;

/**
 * If model does not have version, then find the parent version and use it
 */
public class MissingVersionTransformerRule implements TransformerRule
{
    public void execute(List<ModelProperty> modelProperties, boolean isMostSpecialized) throws DataSourceException
    {        
        if ( PomTransformer.getPropertyFor( ProjectUri.version, modelProperties ) == null )
        {
            ModelProperty parentVersion = PomTransformer.getPropertyFor( ProjectUri.Parent.version, modelProperties );
            if ( parentVersion != null )
            {
                modelProperties.add( new ModelProperty( ProjectUri.version, parentVersion.getResolvedValue() ) );
            }
        }
    }
}
