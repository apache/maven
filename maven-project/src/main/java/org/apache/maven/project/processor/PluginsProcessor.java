package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
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
             
       // Model t = (Model) target;
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
                for ( Plugin d1 : c)
                {
                    for ( Plugin d2 : p)
                    {
                        if ( match( d1, d2 ) )
                        {
                            processor.process( d2, d1, plugins, isChildMostSpecialized );// JOIN
                        }
                        else
                        {
                            processor.process( null, d1, plugins, isChildMostSpecialized );
                            parentDependencies.add( d2 );
                        }
                    }
                }

                for ( Plugin d2 : parentDependencies )
                {
                    processor.process( d2, null, plugins, isChildMostSpecialized );
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
        /*
        if ( getId( d1 ).equals( getId( d2 ) ) )
        {
            if(d1.getVersion() == null || d2.getVersion() == null)
            {
                return true;
            }
            return ( d1.getVersion() == null ? "" : d1.getVersion() ).equals( d2.getVersion() == null ? ""
                            : d2.getVersion() );
        }
        return false;
        */
    }

    private static String getId( Plugin d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() ).append( ":" );
        return sb.toString();
    }    
}
