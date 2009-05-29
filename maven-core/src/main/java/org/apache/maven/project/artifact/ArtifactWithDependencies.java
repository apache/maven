package org.apache.maven.project.artifact;

import java.util.List;

import org.apache.maven.model.Dependency;

public interface ArtifactWithDependencies
{
    List<Dependency> getDependencies();
}
