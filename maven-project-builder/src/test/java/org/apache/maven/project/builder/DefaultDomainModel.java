package org.apache.maven.project.builder;

import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.ModelProperty;

import java.util.List;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;

public class DefaultDomainModel implements IPomClassicDomainModel {

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

    public boolean isPomInBuild() {
        return false;
    }

    public File getProjectDirectory() {
        return null;
    }

    public InputStream getInputStream() {
        return null;
    }
}
