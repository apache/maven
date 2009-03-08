package org.apache.maven.project.processor;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

public class DependencyProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        List<Dependency> t = (List<Dependency>) target;
        
        if (parent == null && child == null)
        {
            return;
        }
        else if(parent == null && child != null)
        {
            Dependency targetDependency = new Dependency();
            copy( (Dependency) child, targetDependency);
            t.add( targetDependency );    
        } 
        else if( parent != null && child == null)
        {
            Dependency targetDependency = new Dependency();
            copy( (Dependency) parent, targetDependency);
            t.add( targetDependency );
        }
        else //JOIN
        {
            Dependency targetDependency = new Dependency();
            copy( (Dependency) child, targetDependency);
            copy( (Dependency) parent, targetDependency);
            t.add( targetDependency );
        }
    }
    
    private static void copy(Dependency dependency, Dependency targetDependency)
    {
        if(targetDependency.getArtifactId() == null)
        {
            targetDependency.setArtifactId( dependency.getArtifactId() );
        }
        
        if(targetDependency.getClassifier() == null)
        {
            targetDependency.setClassifier( dependency.getClassifier() );
        }
        
        if(targetDependency.getGroupId() == null)
        {
            targetDependency.setGroupId(dependency.getGroupId());
        }
        
        if( targetDependency.getScope() == null)
        {
            targetDependency.setScope( dependency.getScope() );
        }
        
        if(targetDependency.getSystemPath() == null)
        {
            targetDependency.setSystemPath( dependency.getSystemPath() );
        }
        
        if( targetDependency.getType() == null )
        {
            targetDependency.setType( dependency.getType() );
        }
        
        if(targetDependency.getVersion() == null)
        {
            targetDependency.setVersion( dependency.getVersion() );
        }
        
        if(!dependency.getExclusions().isEmpty())
        {
            List<Exclusion> targetExclusions = targetDependency.getExclusions();
            for(Exclusion e : dependency.getExclusions())
            {
                if(!containsExclusion(e, targetExclusions))
                {
                    Exclusion e1 = new Exclusion();
                    e1.setArtifactId( e.getArtifactId() );
                    e1.setGroupId( e.getGroupId() );
                    targetExclusions.add( e1 );
                }
            }
        }
    }
    
    private static boolean containsExclusion(Exclusion exclusion, List<Exclusion> exclusions)
    {
        for(Exclusion e :exclusions)
        {
            if(e.getGroupId().equals( exclusion.getGroupId() ) && e.getArtifactId().equals( exclusion.getArtifactId() ))
            {
                return true;
            }
        } 
        return false;
    }
}
