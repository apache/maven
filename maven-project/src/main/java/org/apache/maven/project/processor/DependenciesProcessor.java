package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.ModelContainerAction;

public class DependenciesProcessor
    extends BaseProcessor
{   
    
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        List<Dependency> c = (child != null) ?  (List<Dependency>) child : new ArrayList<Dependency>() ;
        List<Dependency> p = null;
        
        if ( parent != null )
        {
            p = (List<Dependency>) parent;
        }
        List<Dependency> dependencies = (List<Dependency>) target;

        DependencyProcessor processor = new DependencyProcessor();
        if ( ( p == null || p.isEmpty() ) && !c.isEmpty()  )
        {
            for ( Dependency dependency : c )
            {
                processor.process( null, dependency, dependencies, isChildMostSpecialized );
            }
        }
        else
        {
            if ( !c.isEmpty() )
            {
                List<Dependency> parentDependencies = new ArrayList<Dependency>();
                for ( Dependency d1 : c)
                {
                    for ( Dependency d2 : p)
                    {
                        if ( match( d1, d2 ) )
                        {
                            processor.process( d2, d1, dependencies, isChildMostSpecialized );// JOIN
                        }
                        else
                        {
                            processor.process( null, d1, dependencies, isChildMostSpecialized );
                            parentDependencies.add( d2 );
                        }
                    }
                }

                for ( Dependency d2 : parentDependencies )
                {
                    processor.process( d2, null, dependencies, isChildMostSpecialized );
                }
            }
            else if( p != null)
            {
                for ( Dependency d2 : p )
                {
                    processor.process( d2, null, dependencies, isChildMostSpecialized );
                }
            }
        }
        
        //Cleanup duplicates
       // List<Dependency> remove = new ArrayList<Dependency>();
        List<Dependency> ordered = new ArrayList<Dependency>(dependencies);
        Collections.reverse( ordered );
        for(Dependency dependency : ordered)
        {
            for(int i = ordered.indexOf( dependency ) + 1; i < ordered.size(); i++)
            {
                Dependency d1 = ordered.get( i );
                if(match1(d1, dependency))
                {
                    dependencies.remove( dependency );    
                }
            }
        }
       // dependencies.removeAll( remove );
    }

    private static boolean contains(Dependency dependency, List<Dependency> dependencies)
    {
        
        return false;
    }

    private boolean match1(Dependency d1, Dependency d2)
    {
         return getId( d1 ).equals( getId( d2 ) );
    }

    /*
    private boolean match(Dependency d1, Dependency d2)
    {
        if(isManagement)
        {
            return d1.getGroupId().equals( d2.getGroupId() ) && d1.getArtifactId().equals( d2.getArtifactId() );
        }
        else
        {
            return d1.getGroupId().equals( d2.getGroupId() ) && d1.getArtifactId().equals( d2.getArtifactId() ) && d2.getVersion().equals(d1.getVersion());
        }
    }
    */
    private boolean match( Dependency d1, Dependency d2 )
    {
        // TODO: Version ranges ?
        if ( getId( d1 ).equals( getId( d2 ) ) )
        {
            return ( d1.getVersion() == null ? "" : d1.getVersion() ).equals( d2.getVersion() == null ? ""
                            : d2.getVersion() );
        }
        return false;
    }

    private String getId( Dependency d )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() );
        sb.append( ":" ).append(
                                     d.getType() == null ? "jar"
                                                     : "" ).append(
                                                                    ":" ).append(
                                                                                  d.getClassifier() );
        
        return sb.toString();
    }
}
