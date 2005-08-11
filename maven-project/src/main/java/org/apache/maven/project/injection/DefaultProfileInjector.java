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
        // [jc 11-aug-2005] NOTE: the following merge-then-override procedure is used to preserve proper dominance 
        // (profile wins), while ensuring that any changes are pushed to the model.
        ModelUtils.mergeModelBases( profile, model, true );
        
        ModelUtils.overrideModelBase( model, profile );
        
        BuildBase profileBuild = profile.getBuild();
        
        if ( profileBuild != null )
        {
            ModelUtils.mergeBuildBases( profile.getBuild(), model.getBuild() );
            
            Build modelBuild = model.getBuild();
            
            ModelUtils.overrideBuildBase( modelBuild, profileBuild );
        }
    }

}
