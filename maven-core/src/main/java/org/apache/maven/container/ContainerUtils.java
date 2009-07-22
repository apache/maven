package org.apache.maven.container;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ContainerUtils
{
    
    public static Set<String> findChildComponentHints( String role, PlexusContainer parent, PlexusContainer child )
    {
        return findChildComponents( role, parent, child ).keySet();
    }
    
    @SuppressWarnings( "unchecked" )
    public static Map<String, ComponentDescriptor> findChildComponents( String role, PlexusContainer parent, PlexusContainer child )
    {
        Map<String, ComponentDescriptor> parentComponents = parent.getComponentDescriptorMap( role );
        if ( parentComponents != null )
        {
            parentComponents = new LinkedHashMap<String, ComponentDescriptor>( parentComponents );
        }
        
        Map<String, ComponentDescriptor> childComponents = child.getComponentDescriptorMap( role );
        if ( childComponents == null )
        {
            return new HashMap<String, ComponentDescriptor>();
        }
        else
        {
            childComponents = new LinkedHashMap<String, ComponentDescriptor>( childComponents );
            if ( parentComponents != null && !parentComponents.isEmpty() )
            {
                for ( Map.Entry<String, ComponentDescriptor> entry : parentComponents.entrySet() )
                {
                    String hint = entry.getKey();
                    
                    if ( childComponents.containsKey( hint ) && entry.getValue() == childComponents.get( hint ) )
                    {
                        childComponents.remove( hint );
                    }
                }
            }
        }
        
        return childComponents;
    }

}
