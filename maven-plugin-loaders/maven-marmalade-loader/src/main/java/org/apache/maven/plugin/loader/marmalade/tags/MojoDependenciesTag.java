/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.descriptor.Dependency;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

/**
 * @author jdcasey
 */
public class MojoDependenciesTag extends AbstractMarmaladeTag {
    
    private List dependencies = new LinkedList();

    protected void doExecute(MarmaladeExecutionContext context)
            throws MarmaladeExecutionException {
        
        processChildren(context);
        
        MojoDescriptorTag parent = (MojoDescriptorTag)requireParent(MojoDescriptorTag.class);
        parent.setDependencies(dependencies);
    }
    
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }
}
