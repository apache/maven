package org.apache.maven.project.processor;

import org.apache.maven.model.License;
import org.apache.maven.model.Model;

public class LicensesProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
        
        if(c.getLicenses().isEmpty() && p != null)
        {
            for(License license : p.getLicenses())
            {
                License l = new License();
                l.setUrl( license.getUrl());
                l.setDistribution( license.getDistribution() );
                l.setComments( license.getComments() );
                l.setName( license.getName() );
                t.addLicense( l );
            }
        }
        else if(isChildMostSpecialized )
        {
            for(License license : c.getLicenses())
            {
                License l = new License();
                l.setUrl( license.getUrl());
                l.setDistribution( license.getDistribution() );
                l.setComments( license.getComments() );
                l.setName( license.getName() );
                t.addLicense( l );
            }           
        }
    }
}
