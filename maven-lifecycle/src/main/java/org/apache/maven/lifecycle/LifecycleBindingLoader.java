package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.LifecycleBindings;

public interface LifecycleBindingLoader
{
    
    String ROLE = LifecycleBindingLoader.class.getName();

    LifecycleBindings getBindings()
        throws LifecycleLoaderException, LifecycleSpecificationException;

}
