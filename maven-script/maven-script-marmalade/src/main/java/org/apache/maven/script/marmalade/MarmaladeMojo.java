/* Created on Aug 9, 2004 */
package org.apache.maven.script.marmalade;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.FailureResponse;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.runtime.DefaultContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

/**
 * @author jdcasey
 */
public class MarmaladeMojo extends AbstractPlugin {
    
    private MarmaladeScript script;

    public MarmaladeMojo(MarmaladeScript script) {
        this.script = script;
    }

    public void execute(PluginExecutionRequest request,
            PluginExecutionResponse response) throws Exception {
        
        MarmaladeExecutionContext context = new DefaultContext();
        
        context.setVariable(MarmaladeMojoExecutionDirectives.REQUEST_INVAR, request);
        context.setVariable(MarmaladeMojoExecutionDirectives.RESPONSE_INVAR, response);
        
        try
        {
            script.execute( context );
        }
        catch( MarmaladeExecutionException e )
        {
            FailureResponse failure = new MarmaladeMojoFailureResponse(script.getLocation(), e);            
            response.setExecutionFailure(true, failure);
        }
    }

}
