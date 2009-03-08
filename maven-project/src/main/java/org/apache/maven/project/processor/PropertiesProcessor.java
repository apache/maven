package org.apache.maven.project.processor;

import java.util.Properties;

import org.apache.maven.model.Model;

public class PropertiesProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target, c = (Model) child, p = (Model) parent;

        Properties properties = new Properties();
        if ( p != null && p.getProperties() != null )
        {
            properties.putAll( p.getProperties() );
        }
        
        if ( c.getProperties() != null )
        {
            properties.putAll( c.getProperties() );
        }

        if ( !properties.isEmpty() )
        {
            t.setProperties( properties );
        }
    }
}
