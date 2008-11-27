package org.apache.maven.project.builder;

import static org.junit.Assert.*;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class PluginSpecTest {

    @org.junit.Test
    public void goalsInherited() {
        List<ModelProperty> mp = new ArrayList<ModelProperty>();
        mp.add(new ModelProperty(ProjectUri.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.groupId, "gid"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.artifactId, "aid"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.version, "v1"));

        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.xUri, null));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.id, "site-docs"));
        mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.phase, "phase"));
       // mp.add(new ModelProperty(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.goals, null));
        /*
              <phase>pre-site</phase>
              <goals>
                <goal>xdoc</goal>
                <goal>xsd</goal>
              </goals>
            </execution>
            <execution>
              <id>standard</id>
              <goals>
                <goal>java</goal>
                <goal>xpp3-reader</goal>
                <goal>xpp3-writer</goal>
              </goals>
            </execution>


        List<ModelProperty> mp2 = new ArrayList<ModelProperty>();
        mp2.add(new ModelProperty(ProjectUri.xUri, null));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.xUri, null));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.xUri, null));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.groupId, "gid"));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.artifactId, "aid"));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.xUri, null));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.groupId, "gid"));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.artifactId, "aid"));
        mp2.add(new ModelProperty(ProjectUri.Dependencies.Dependency.classifier, "tests"));

        DomainModel childModel = new DefaultDomainModel(mp2);
        DomainModel parentModel = new DefaultDomainModel(mp);

        ModelTransformerContext ctx = new ModelTransformerContext(Arrays.asList(new ArtifactModelContainerFactory(),
                new IdModelContainerFactory()));

        ModelTransformer transformer = new PomTransformer(new DefaultDomainModelFactory());
        DomainModel domainModel = ctx.transform( Arrays.asList(childModel, parentModel), transformer, transformer );

        DefaultModelDataSource source = new DefaultModelDataSource();
        source.init(domainModel.getModelProperties(), Arrays.asList(new ArtifactModelContainerFactory(), new IdModelContainerFactory()));
        List<ModelContainer> containers = source.queryFor(ProjectUri.Dependencies.Dependency.xUri);
        */
    }

}
