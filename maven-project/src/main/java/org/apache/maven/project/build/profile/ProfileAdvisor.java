package org.apache.maven.project.build.profile;

import org.apache.maven.model.Model;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 
 * @author jdcasey
 *
 */
public interface ProfileAdvisor
{
    
    String ROLE = ProfileAdvisor.class.getName();
    
    LinkedHashSet getArtifactRepositoriesFromActiveProfiles( Model model, File projectDir, List explicitlyActiveIds, List explicitlyInactiveIds )
        throws ProjectBuildingException;
    
    List applyActivatedProfiles( Model model, File projectDir, List explicitlyActiveIds, List explicitlyInactiveIds )
        throws ProjectBuildingException;

    List applyActivatedExternalProfiles( Model model, File projectDir, ProfileManager externalProfileManager )
        throws ProjectBuildingException;

}
