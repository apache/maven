package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;

public class ProfilesModuleProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
        List<String> modules = new ArrayList<String>();



        for ( String module : c.getModules() )
        {
            if(!modules.contains( module ))
            {
                modules.add(module);
            }
        }       
        if(p != null)
        {
            for ( String module : p.getModules() )
            {
                if(!modules.contains( module ))
                {
                    modules.add(module);
                }
            }           
        }       
        t.setModules( modules );
    }
}
