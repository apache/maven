package org.apache.maven.project.builder;

import static org.junit.Assert.*;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

public class PluginSpecTest {

    @org.junit.Test
    public void goalsInherited() throws IOException {

        List<ModelProperty> mp0 = new ArrayList<ModelProperty>();
        mp0.add(new ModelProperty(ProjectUri.xUri, null));
        mp0.add(new ModelProperty(ProjectUri.Build.xUri, null));
        mp0.add(new ModelProperty(ProjectUri.Build.PluginManagement.xUri, null));
        mp0.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.xUri, null));
        mp0.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri, null));
        mp0.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.groupId, "org.codehaus.modello"));
        mp0.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.artifactId, "modello-maven-plugin"));
        mp0.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.version, "v1"));

        List<ModelProperty> mp = new ArrayList<ModelProperty>();
        mp.add(new ModelProperty(ProjectUri.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.groupId, "org.codehaus.modello"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.artifactId, "modello-maven-plugin"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.version, "v1"));

        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.id, "site-docs"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.phase, "phase"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.xURI, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.goal, "xdoc"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.goal, "xsd"));

        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.id, "standard"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.xURI, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.goal, "xpp3-reader"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.goal, "xpp3-writer"));

        DomainModel parentModel = new DefaultDomainModel(mp);

        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );

        ModelTransformer transformer = new PomTransformer(new DefaultDomainModelFactory());
        DomainModel domainModel = ctx.transform( Arrays.asList(parentModel, new DefaultDomainModel(mp0)), transformer, transformer );


        List<ModelContainerFactory> factories = new ArrayList<ModelContainerFactory>(PomTransformer.MODEL_CONTAINER_FACTORIES);
        factories.add(new PluginExecutionIdModelContainerFactory());
        DefaultModelDataSource source = new DefaultModelDataSource(domainModel.getModelProperties(), factories);

        List<ModelContainer> containers = source.queryFor(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.xUri);
        assertTrue(2 == containers.size());

        int numberOfGoals = 0;
        for(ModelProperty x : containers.get(0).getProperties())
        {
            if(x.getUri().equals(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.goal))
            {
                numberOfGoals++;
            }
        }
        assertTrue(numberOfGoals == 2);

        numberOfGoals = 0;
        for(ModelProperty x : containers.get(1).getProperties())
        {
            if(x.getUri().equals(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.Goals.goal))
            {
                numberOfGoals++;
            }
        }
        assertTrue(numberOfGoals == 2);

      //  System.out.println(ModelMarshaller.unmarshalModelPropertiesToXml(domainModel.getModelProperties(), ProjectUri.baseUri));


    }

}
