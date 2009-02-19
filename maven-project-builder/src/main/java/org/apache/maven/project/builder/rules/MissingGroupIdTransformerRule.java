package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.TransformerRule;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;

public class MissingGroupIdTransformerRule implements TransformerRule
{
    public void execute(List<ModelProperty> modelProperties, int domainIndex) throws DataSourceException
    {

    }
}
