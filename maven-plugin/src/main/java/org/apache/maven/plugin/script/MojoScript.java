package org.apache.maven.plugin.script;

import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * @author jdcasey
 */
public interface MojoScript
{
    
    MojoDescriptor getMojoDescriptor();
    
}
