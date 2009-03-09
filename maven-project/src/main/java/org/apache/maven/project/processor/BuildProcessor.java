package org.apache.maven.project.processor;

import java.util.Collection;

public class BuildProcessor
    extends BaseProcessor
{
    public BuildProcessor( Collection<Processor> processors )
    {
        super( processors );
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
    }
}
