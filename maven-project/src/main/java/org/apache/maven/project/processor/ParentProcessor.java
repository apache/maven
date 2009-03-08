package org.apache.maven.project.processor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

public class ParentProcessor
    extends BaseProcessor
{

    public ParentProcessor()
    {
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        if(c.getParent() != null)
        {
            Parent p = new Parent();
            p.setGroupId( c.getParent().getGroupId() );
            p.setArtifactId( c.getParent().getArtifactId() );
            p.setVersion( c.getParent().getVersion() );
            p.setRelativePath( c.getParent().getRelativePath() );
            t.setParent( p );           
        }
    }
}
