package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.TransformerRemovalRule;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;
import java.util.ArrayList;

/**
 * If the model is not the least child and has a module element, remove it.
 */
public class ModulesNotInheritedTransformerRule implements TransformerRemovalRule
{
    public List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, int domainIndex)
            throws DataSourceException
    {
        if (domainIndex > 0)
        {
            ModelProperty modulesProperty = PomTransformer.getPropertyFor(ProjectUri.Modules.xUri, modelProperties);
            if (modulesProperty != null)
            {
                modelProperties.remove(modulesProperty);
                modelProperties.removeAll(PomTransformer.getPropertiesFor(ProjectUri.Modules.module, modelProperties));
            }
        }
        return new ArrayList<ModelProperty>();//todo: fix
    }
}
