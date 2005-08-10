package org.apache.maven.project.injection;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

public interface ProfileInjector
{
    
    String ROLE = ProfileInjector.class.getName();
    
    void inject( Profile profile, Model model );

}
