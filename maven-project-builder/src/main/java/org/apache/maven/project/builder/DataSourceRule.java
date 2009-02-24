package org.apache.maven.project.builder;

import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.DataSourceException;

public interface DataSourceRule 
{
    void execute(ModelDataSource dataSource) throws DataSourceException;
}
