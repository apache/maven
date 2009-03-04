package org.apache.maven.project.builder;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;

public interface TransformerRule
{
    void execute(List<ModelProperty> modelProperties, boolean isMostSpecialized) throws DataSourceException;

}
