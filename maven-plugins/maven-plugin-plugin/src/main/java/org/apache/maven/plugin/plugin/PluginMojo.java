package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

/**
 * @goal plugin
 *
 * @description Creates a plugin jar
 *
 * @prereq plugin:descriptor
 *
 * @prereq jar:jar
 *
 *
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class PluginMojo  extends AbstractPlugin
{

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response ) throws Exception
    {

    }
}
