package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.shared.model.ModelProperty;

import java.util.ArrayList;
import java.util.List;


public class DefaultModelContainer implements ModelContainer
{

    List<ModelProperty> modelProperties;
    
    public DefaultModelContainer(List<ModelProperty> properties) {
        this.modelProperties = properties;
    }

    public List<ModelProperty> getProperties() {
        return new ArrayList<ModelProperty>(modelProperties);
    }

    public ModelContainerAction containerAction(ModelContainer modelContainer) {
        return null;
    }

    public ModelContainer createNewInstance(List<ModelProperty> modelProperties) {
        return null;
    }
}
