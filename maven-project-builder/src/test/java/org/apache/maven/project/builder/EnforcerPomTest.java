package org.apache.maven.project.builder;

import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

import static org.junit.Assert.*;

public class EnforcerPomTest
{
    @org.junit.Test
    public void dependencyManagementWithScopeAndClassifier() throws IOException
    {
        List<ModelProperty> mp = new ArrayList<ModelProperty>();
        mp.add(new ModelProperty(ProjectUri.xUri, null));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.xUri, null));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.xUri, null));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.xUri, null));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.groupId, "gid"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.artifactId, "aid"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.version, "v1"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.scope, "test"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.classifier, "tests"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.xUri, null));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.groupId, "gid"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.artifactId, "aid"));
        mp.add(new ModelProperty(ProjectUri.DependencyManagement.Dependencies.Dependency.version, "v1"));

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

        ModelTransformerContext ctx = new ModelTransformerContext(PomTransformer.MODEL_CONTAINER_INFOS );

        ModelTransformer transformer = new PomTransformer(new DefaultDomainModelFactory());
        DomainModel domainModel = ctx.transform( Arrays.asList(childModel, parentModel), transformer, transformer );

        DefaultModelDataSource source = new DefaultModelDataSource( domainModel.getModelProperties(), PomTransformer.MODEL_CONTAINER_FACTORIES);

        List<ModelContainer> containers = source.queryFor(ProjectUri.Dependencies.Dependency.xUri);
        assertTrue(containers.size() == 2 );

        ModelContainer mc0 = containers.get(0);
        assertTrue(contains(ProjectUri.Dependencies.Dependency.version, "v1", mc0));
        assertFalse(contains(ProjectUri.Dependencies.Dependency.classifier, "tests", mc0));

        ModelContainer mc1 = containers.get(1);
        assertTrue(contains(ProjectUri.Dependencies.Dependency.version, "v1", mc1));
        assertTrue(contains(ProjectUri.Dependencies.Dependency.classifier, "tests", mc1));
    }

    private boolean contains(String name, String value, ModelContainer modelContainer) {
        for(ModelProperty mp : modelContainer.getProperties()) {
            if(mp.getUri().equals(name) && mp.getValue() != null && mp.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
