package org.apache.maven.project.builder;

import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.ModelProperty;

import java.util.List;
import java.io.IOException;

public class DefaultDomainModel implements DomainModel {

    private List<ModelProperty> modelProperties;

    public DefaultDomainModel(List<ModelProperty> modelProperties) {
        this.modelProperties = modelProperties;
    }

    public List<ModelProperty> getModelProperties() throws IOException {
        return modelProperties;
    }

    public String getEventHistory() {
        return "";
    }

    public void setEventHistory(String s) {

    }
}
