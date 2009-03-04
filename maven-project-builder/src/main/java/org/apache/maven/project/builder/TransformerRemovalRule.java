package org.apache.maven.project.builder;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;

import java.util.List;

public interface TransformerRemovalRule {

        List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, boolean isMostSpecialized)
            throws DataSourceException;
}
