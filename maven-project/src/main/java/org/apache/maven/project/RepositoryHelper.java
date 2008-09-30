package org.apache.maven.project;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.profiles.activation.ProfileActivationContext;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * This is a temporary class. These methods are originally from the DefaultMavenProjectHelper. This class will be
 * eliminated when Mercury is integrated.
 */
public interface RepositoryHelper
{

    String ROLE = RepositoryHelper.class.getName();

    void findModelFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                   ArtifactRepository localRepository )
        throws ProjectBuildingException;

    List buildArtifactRepositories( Model model )
        throws ProjectBuildingException;

    LinkedHashSet collectInitialRepositories( Model model, Model superModel, List parentSearchRepositories,
                                              File pomFile, boolean validProfilesXmlLocation,
                                              ProfileActivationContext profileActivationContext )
        throws ProjectBuildingException;
}
