package org.apache.maven.shared.model;

import java.util.List;
import java.util.Collection;

public interface ModelEventListener {

    public void fire(List<ModelContainer> modelContainers);

    List<String> getUris();
    
    Collection<ModelContainerFactory> getModelContainerFactories();
}
