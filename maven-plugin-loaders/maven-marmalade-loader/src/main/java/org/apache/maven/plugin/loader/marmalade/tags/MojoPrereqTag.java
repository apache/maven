/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import org.codehaus.marmalade.el.ExpressionEvaluationException;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

public class MojoPrereqTag extends AbstractMarmaladeTag {
    
    private static final String NAME_ATTRIBUTE = "name";

    protected boolean alwaysProcessChildren() {
        // shouldn't even have children...
        return false;
    }
    
    protected void doExecute(MarmaladeExecutionContext context)
            throws MarmaladeExecutionException {
        
        String prereq = (String)requireTagAttribute(NAME_ATTRIBUTE, String.class, context);
        
        MojoPrereqsTag parent = (MojoPrereqsTag)requireParent(MojoPrereqsTag.class);
        parent.addPrereq(prereq);
    }
}
