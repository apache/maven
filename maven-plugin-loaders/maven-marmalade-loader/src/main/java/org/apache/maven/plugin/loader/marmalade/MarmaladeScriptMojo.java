/* Created on Aug 9, 2004 */
package org.apache.maven.plugin.loader.marmalade;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.runtime.DefaultContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;

/**
 * @author jdcasey
 */
public class MarmaladeScriptMojo extends AbstractPlugin {
    
    public static final String REQUEST_VARIABLE = "request";
    public static final String RESPONSE_VARIABLE = "response";
    
    private MarmaladeScript script;

    public MarmaladeScriptMojo(MarmaladeScript script) {
        this.script = script;
    }

    public void execute(PluginExecutionRequest request,
            PluginExecutionResponse response) throws Exception {
        
        MarmaladeExecutionContext context = new DefaultContext();
        context.setVariable(REQUEST_VARIABLE, request);
        context.setVariable(RESPONSE_VARIABLE, response);
        
        script.execute(context);
    }

}
