/* Created on Jul 18, 2004 */
package org.apache.maven.plugin;

/**
 * @author jdcasey
 */
public class GoalDecorationAndResolutionTestPlugin implements Plugin
{

    private boolean executed = false;

    public void execute(PluginExecutionRequest request, PluginExecutionResponse response) throws Exception {
        this.executed = true;
    }
    
    public boolean executed() {
        return executed;
    }

}
