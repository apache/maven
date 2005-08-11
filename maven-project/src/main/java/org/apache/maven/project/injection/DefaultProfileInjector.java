package org.apache.maven.project.injection;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.ModelUtils;

public class DefaultProfileInjector
    implements ProfileInjector
{

    public void inject( Profile profile, Model model )
    {
        ModelUtils.mergeModelBases( model, profile );

        BuildBase profileBuild = profile.getBuild();
        if ( profileBuild != null )
        {
            ModelUtils.mergeBuildBases( profile.getBuild(), model.getBuild() );
            
            Build modelBuild = model.getBuild();
            
            modelBuild.setDefaultGoal( profileBuild.getDefaultGoal() );
            modelBuild.setFinalName( profileBuild.getFinalName() );
            modelBuild.setPluginManagement( profileBuild.getPluginManagement() );
            
            modelBuild.setPlugins( profileBuild.getPlugins() );
            modelBuild.flushPluginMap();
            
            modelBuild.setResources( profileBuild.getResources() );
            modelBuild.setTestResources( profileBuild.getTestResources() );
        }
    }

}
