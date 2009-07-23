package org.apache.maven.container;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
