package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Collection;

public abstract class BaseProcessor
{

    Object parent;

    Object child;

    Collection<Processor> processors;

    public BaseProcessor( Collection<Processor> processors )
    {
        if ( processors == null )
        {
            throw new IllegalArgumentException( "processors: null" );
        }

        this.processors = processors;
    }

    public BaseProcessor()
    {
        this.processors = new ArrayList<Processor>();
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        if ( target == null )
        {
            throw new IllegalArgumentException( "target: null" );
        }

        this.parent = parent;
        this.child = child;

        for ( Processor processor : processors )
        {
            processor.process( parent, child, target, isChildMostSpecialized );
        }

    }

    public Object getChild()
    {
        return child;
    }

    public Object getParent()
    {
        return parent;
    }
}
