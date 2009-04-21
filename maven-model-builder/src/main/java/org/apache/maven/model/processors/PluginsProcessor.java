package org.apache.maven.model.processors;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;

public class PluginsProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        List<Plugin> c = (child != null) ?  (List<Plugin>) child : new ArrayList<Plugin>() ;
        List<Plugin> p = null;
        
        if ( parent != null )
        {
            p = (List<Plugin>) parent;
        }

        List<Plugin> plugins = (List<Plugin>) target;    
        
        PluginProcessor processor = new PluginProcessor();

        if ( ( p == null || p.isEmpty() ) && !c.isEmpty()  )
        {
            for ( Plugin plugin : c )
            {
                processor.process( null, plugin, plugins, isChildMostSpecialized );
            }
        }
        else
        {
            if ( !c.isEmpty() )
            {
                List<Plugin> parentDependencies = new ArrayList<Plugin>();
                for ( Plugin childPlugin : c)
                {
                    for ( Plugin parentPlugin : p)
                    {
                        if ( match( childPlugin, parentPlugin ) )
                        {
                            processor.process( parentPlugin, childPlugin, plugins, isChildMostSpecialized );// JOIN
                        }
                        else
                        {
                            processor.process( null, childPlugin, plugins, isChildMostSpecialized );
                            if(!parentDependencies.contains(parentPlugin))
                            	{
                            	parentDependencies.add( parentPlugin );
                            }
                        }
                    }
                }
                
                /**
                 * Process Parents after child to keep plugin order but don't want to overwrite the child values. Use different method
                 */
                for ( Plugin d2 : parentDependencies )
                {
                    processor.process( d2, plugins, isChildMostSpecialized );
                }
                
            }
            else if( p != null)
            {
                for ( Plugin d2 : p )
                {
                    processor.process( d2, null, plugins, isChildMostSpecialized );
                }
            }
        }      
        
    }
    
    private static boolean match( Plugin d1, Plugin d2 )
    {
        return getId( d1 ).equals( getId( d2 ));
    }

    private static String getId( Plugin d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( (d.getGroupId() != null) ? d.getGroupId() : "org.apache.maven.plugins").append( ":" ).append( d.getArtifactId() ).append( ":" );
        return sb.toString();
    }    
}
