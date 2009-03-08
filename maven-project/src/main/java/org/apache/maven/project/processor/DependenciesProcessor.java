package org.apache.maven.project.processor;

import java.util.ArrayList;
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
        Model c = (Model) child;
        Model p = null;
        if ( parent != null )
        {
            p = (Model) parent;
        }
        List<Dependency> dependencies = ((Model) target).getDependencies();
        
        DependencyProcessor processor = new DependencyProcessor();
        if ( ( p == null || p.getDependencies().isEmpty() ) && !c.getDependencies().isEmpty())
        {
            for ( Dependency dependency : c.getDependencies() )
            {
                processor.process( null, dependency, dependencies, isChildMostSpecialized );
            }
        }
        else
        {
            if ( !c.getDependencies().isEmpty() )
            {
                List<Dependency> parentDependencies = new ArrayList<Dependency>();
                for ( Dependency d1 : c.getDependencies() )
                {
                    for ( Dependency d2 : p.getDependencies() )
                    {
                        if(match(d1, d2))
                        {                            
                            processor.process( d2, d1, dependencies, isChildMostSpecialized );//JOIN
                        }
                        else
                        {
                            processor.process( null, d1, dependencies, isChildMostSpecialized );
                            parentDependencies.add( d2 );
                        }
                    }
                }
                
                for(Dependency d2 : parentDependencies)
                {
                    processor.process( d2, null, dependencies, isChildMostSpecialized );    
                }
            }
            else
            {
                for(Dependency d2 :  p.getDependencies())
                {
                    processor.process( d2, null, dependencies, isChildMostSpecialized );    
                }               
            }
        }
    }
    
    private static boolean match( Dependency d1, Dependency d2 )
    {
        // TODO: Version ranges ?
        if ( getId( d1 ).equals( getId( d2 ) ) )
        {
            return ( d1.getVersion() == null ? "" : d1.getVersion() ).equals( d2.getVersion() == null ? ""
                            : d2.getVersion() );
        }
        return false;
    }

    private static String getId( Dependency d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() ).append( ":" ).append(
                                                                                                    d.getType() == null ? "jar"
                                                                                                                    : "" ).append(
                                                                                                                                   ":" ).append(
                                                                                                                                                 d.getClassifier() );
        return sb.toString();
    }      
}
