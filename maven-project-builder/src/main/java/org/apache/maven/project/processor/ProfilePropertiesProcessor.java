package org.apache.maven.project.processor;

import java.util.Properties;

import org.apache.maven.model.Model;

public class ProfilePropertiesProcessor    
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target, c = (Model) child, p = (Model) parent;

        Properties properties = new Properties();
               
        if ( c.getProperties() != null )
        {
            properties.putAll( c.getProperties() );
        }
        
        if ( p != null && p.getProperties() != null )
        {
            properties.putAll( p.getProperties() );
        }
        
        if ( !properties.isEmpty() )
        {
            if(t.getProperties().isEmpty())
            {
                t.setProperties( properties );   
            }
            else
            {
                t.getProperties().putAll( properties );
            }       
        }
    }

}
