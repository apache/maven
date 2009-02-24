package org.apache.maven.project.builder.rules;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.TransformerRemovalRule;

import java.util.List;
import java.util.ArrayList;

/**
 * Removes any plugin execution id that has a value of "default-execution-id": (mng-3965)
 */
public class DefaultExecutionIdTransformerRule implements TransformerRemovalRule
{
    public List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, int domainIndex)
            throws DataSourceException
    {
        List<ModelProperty> replace = new ArrayList<ModelProperty>();
        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().equals(ProjectUri.Build.Plugins.Plugin.Executions.Execution.id)
                    && mp.getResolvedValue() != null && mp.getResolvedValue().equals("default-execution-id")) {
                replace.add(mp);
            }
        }
        return replace;
    }
}
