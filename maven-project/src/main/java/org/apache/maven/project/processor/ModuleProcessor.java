package org.apache.maven.project.processor;

import java.util.ArrayList;

import org.apache.maven.model.Model;

public class ModuleProcessor
    extends BaseProcessor
{

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        if ( isChildMostSpecialized )
        {
            Model t = (Model) target;
            Model c = (Model) child;
            t.setModules( new ArrayList<String>( c.getModules() ) );
        }
    }

}
