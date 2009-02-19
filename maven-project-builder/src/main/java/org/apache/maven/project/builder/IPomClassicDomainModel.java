package org.apache.maven.project.builder;

import org.apache.maven.shared.model.InputStreamDomainModel;

import java.io.File;

public interface IPomClassicDomainModel extends InputStreamDomainModel
{
    boolean isPomInBuild();

    File getProjectDirectory();
}
