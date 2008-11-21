package org.apache.maven.shared.model;

import java.util.List;
import java.io.IOException;

public interface DomainModelFactory {

    DomainModel createDomainModel(List<ModelProperty> modelProperties) throws IOException;
}
