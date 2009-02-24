package org.apache.maven.project.builder;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;

public interface JoinRule
{
    List<ModelProperty> execute(List<ModelProperty> modelProperties) throws DataSourceException;
}
