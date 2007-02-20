package org.apache.maven.extension;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

import java.io.File;
import java.util.List;

public interface BuildExtensionScanner
{
    
    String ROLE = BuildExtensionScanner.class.getName();
    
    void scanForBuildExtensions( List files, ArtifactRepository localRepository, ProfileManager globalProfileManager )
        throws ExtensionScanningException;

    void scanForBuildExtensions( File pom, ArtifactRepository localRepository, ProfileManager globalProfileManager )
        throws ExtensionScanningException;

}
