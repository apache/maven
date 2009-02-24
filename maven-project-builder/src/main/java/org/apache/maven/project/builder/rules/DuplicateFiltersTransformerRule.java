package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.TransformerRemovalRule;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;
import java.util.ArrayList;

public class DuplicateFiltersTransformerRule implements TransformerRemovalRule
{
    public List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, int domainIndex)
            throws DataSourceException
    {
        List<ModelProperty> removedProperties = new ArrayList<ModelProperty>();
        List<String> filters = new ArrayList<String>();
        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().equals(ProjectUri.Build.Filters.filter))
            {
                if(filters.contains(mp.getResolvedValue()))
                {
                    removedProperties.add(mp);
                }
                else
                {
                    filters.add(mp.getResolvedValue());
                }
            }
        }
        return removedProperties;
    }
}
