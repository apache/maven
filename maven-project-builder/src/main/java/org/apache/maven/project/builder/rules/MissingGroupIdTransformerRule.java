package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.TransformerRule;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;
import static org.apache.maven.project.builder.PomTransformer.getPropertyFor;

import java.util.List;

/**
 * If the groupId is missing, add it using the value of the parent groupId.
 */
public class MissingGroupIdTransformerRule implements TransformerRule
{
    public void execute(List<ModelProperty> modelProperties, boolean isMostSpecialized) throws DataSourceException
    {
        if ( getPropertyFor( ProjectUri.groupId, modelProperties ) == null )
        {
            ModelProperty parentGroupId = getPropertyFor( ProjectUri.Parent.groupId, modelProperties );
            if ( parentGroupId != null )
            {
                modelProperties.add( new ModelProperty( ProjectUri.groupId, parentGroupId.getResolvedValue() ) );
            }
        }
    }
}
