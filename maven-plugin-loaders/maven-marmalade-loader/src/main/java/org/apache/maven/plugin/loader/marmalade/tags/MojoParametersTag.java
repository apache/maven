/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

/**
 * @author jdcasey
 */
public class MojoParametersTag extends AbstractMarmaladeTag {
    
    private List parameters = new LinkedList();

    protected void doExecute(MarmaladeExecutionContext context)
            throws MarmaladeExecutionException {
        
        processChildren(context);
        
        MojoDescriptorTag parent = (MojoDescriptorTag)requireParent(MojoDescriptorTag.class);
        parent.setParameters(parameters);
    }
    
    public void addParameter(Parameter param) {
        parameters.add(param);
    }
}
