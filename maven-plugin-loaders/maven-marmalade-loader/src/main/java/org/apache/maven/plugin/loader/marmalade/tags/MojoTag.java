/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.loader.marmalade.MarmaladeScriptMojo;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.model.MarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.plexus.component.factory.marmalade.PlexusComponentTag;

/**
 * @author jdcasey
 */
public class MojoTag extends AbstractMarmaladeTag implements PlexusComponentTag
{
    private boolean describeOnly = false;
    
    private List dependencies;
    private MojoDescriptor descriptor;
    
    private MarmaladeScriptMojo mojo; 

    protected boolean alwaysProcessChildren(  )
    {
        return false;
    }

    protected void doExecute( MarmaladeExecutionContext context )
        throws MarmaladeExecutionException
    {
        boolean describeOnly = describeOnly();
        if(describeOnly) {
            for (Iterator it = children().iterator(); it.hasNext();) {
                MarmaladeTag child = (MarmaladeTag) it.next();
                
                if(child instanceof MojoDescriptorTag) {
                    MojoDescriptorTag headerTag = (MojoDescriptorTag)child;
                    child.execute(context);
                    
                    this.descriptor = headerTag.getMojoDescriptor();
                    this.dependencies = headerTag.getDependencies();
                    
                    // we're done with the description phase.
                    break;
                }
            }
        }
        else {
            MarmaladeScript script = new MarmaladeScript(getTagInfo().getSourceFile(), this);
            this.mojo = new MarmaladeScriptMojo(script);
        }
    }

    public MojoDescriptor getMojoDescriptor() {
        return descriptor;
    }

    public List addDependencies(List accumulatedDependencies) {
        accumulatedDependencies.addAll(dependencies);
        return accumulatedDependencies;
    }
    
    public void describeOnly(boolean describeOnly) {
        this.describeOnly = describeOnly;
    }
    
    public boolean describeOnly() {
        return describeOnly;
    }

    public Object getComponent() {
        return mojo;
    }
    
}
