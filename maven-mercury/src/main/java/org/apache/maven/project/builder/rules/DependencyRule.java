package org.apache.maven.project.builder.rules;

import org.apache.maven.shared.model.ModelContainerRule;
import org.apache.maven.shared.model.ModelProperty;

import java.util.List;
import java.util.ArrayList;

public class DependencyRule implements ModelContainerRule {
    public List<ModelProperty> execute(List<ModelProperty> modelProperties) {
        List<ModelProperty> properties = new ArrayList<ModelProperty>(modelProperties);
        List<ModelProperty> goalProperties = new ArrayList<ModelProperty>();
        List<ModelProperty> processedProperties = new ArrayList<ModelProperty>();

        return processedProperties;

    }
}
