package org.apache.maven.plugin;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.execution.MavenSession;
import org.apache.commons.jxpath.JXPathContext;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.io.StringReader;

public class DefaultPluginContext implements PluginContext {

    @Requirement
    protected MavenPluginCollector pluginCollector;

    @Requirement
    protected PluginManager pluginManager;

    public Collection<MojoExecution> getMojoExecutionsForGoal(String goal) throws Exception
    {
        List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

        for(PluginDescriptor descriptor : pluginCollector.getPluginDescriptors())
        {
            MojoDescriptor mojoDescriptor = descriptor.getMojo(goal);
            if(mojoDescriptor != null)
            {
                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor ); 
                mojoExecution.setConfiguration(
                        Xpp3DomBuilder.build( new StringReader( mojoDescriptor.getMojoConfiguration().toString() ) ) );
                mojoExecutions.add(mojoExecution);
            }
        }
        
        return mojoExecutions;
    }

    public Object getMojoParameterFor(MojoExecution mojoExecution, String xPath) throws Exception {
        Xpp3Dom mojoDescriptorConfiguration =
                Xpp3DomBuilder.build( new StringReader(mojoExecution.getMojoDescriptor().getMojoConfiguration().toString()));

        Xpp3Dom mergedConfig = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoDescriptorConfiguration );
        return JXPathContext.newContext( mergedConfig ).getValue( xPath );
    }

    public void executeMojo( MojoExecution mojoExecution, MavenSession session ) throws Exception
    {
        pluginManager.executeMojo(session.getCurrentProject(), mojoExecution, session);
    }
}
