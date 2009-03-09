package org.apache.maven.project.processor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;

public class PrerequisitesProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        if ( isChildMostSpecialized )
        {
            Model t = (Model) target;
            Model c = (Model) child;
            if ( c.getPrerequisites() == null )
            {
                return;
            }
            Prerequisites prerequisites = new Prerequisites();
            prerequisites.setMaven( c.getPrerequisites().getMaven() );
            t.setPrerequisites( prerequisites );
        }
    }
}
