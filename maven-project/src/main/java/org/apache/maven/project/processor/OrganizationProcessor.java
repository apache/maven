package org.apache.maven.project.processor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;

public class OrganizationProcessor
    extends BaseProcessor
{

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;

        if ( c.getOrganization() != null )
        {
            copy( c.getOrganization(), t );
        }
        else if ( p != null && p.getOrganization() != null )
        {
            copy( p.getOrganization(), t );
        }
    }

    private static void copy( Organization source, Model target )
    {
        Organization o = new Organization();
        o.setName( source.getName() );
        o.setUrl( source.getUrl() );

        target.setOrganization( o );
    }
}
