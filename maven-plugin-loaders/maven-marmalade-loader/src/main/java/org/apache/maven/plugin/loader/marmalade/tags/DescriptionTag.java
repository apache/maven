/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import org.codehaus.marmalade.el.ExpressionEvaluationException;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

public class DescriptionTag extends AbstractMarmaladeTag
{
    protected void doExecute( MarmaladeExecutionContext context )
        throws MarmaladeExecutionException
    {
        String description = (String)getBody(context, String.class);
        
        DescriptionOwner parent = (DescriptionOwner)requireParent(DescriptionOwner.class);
        
        parent.setDescription(description);
    }

    protected boolean preserveBodyWhitespace( MarmaladeExecutionContext arg0 )
        throws ExpressionEvaluationException
    {
        return false;
    }
}
