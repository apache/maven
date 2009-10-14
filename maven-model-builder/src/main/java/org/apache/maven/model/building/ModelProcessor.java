package org.apache.maven.model.building;

import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;

public interface ModelProcessor
    extends ModelLocator, ModelReader
{
    String LOCATION = "org.apache.maven.model.building.location";

    String SOURCE = "org.apache.maven.model.building.source";
}
