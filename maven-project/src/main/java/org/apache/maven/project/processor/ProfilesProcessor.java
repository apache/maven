package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.builder.PomClassicDomainModel;

public class ProfilesProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        List<Profile> c = (List<Profile>) child;
        List<PomClassicDomainModel> models = new ArrayList<PomClassicDomainModel>();
        for(Profile profile : c)
        {
           // models.add( new PomClassicDomainModel )
            //copy(profile, t);
        }   
    }
    
}
