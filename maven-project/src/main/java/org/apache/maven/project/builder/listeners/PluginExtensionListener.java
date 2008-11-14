package org.apache.maven.project.builder.listeners;

import org.apache.maven.shared.model.ModelEventListener;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;

import java.util.List;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;

public class PluginExtensionListener implements ModelEventListener {

    public void fire(List<ModelContainer> modelContainers) {
        List<Plugin> pluginsWithExtension = new ArrayList<Plugin>();
        for (ModelContainer mc : modelContainers) {
            if(hasExtension(mc)) {
                pluginsWithExtension.add(new Plugin(mc.getProperties()));
            }
        }

        //Do something with plugins here
    }

    public List<String> getUris() {
        return Arrays.asList(ProjectUri.Build.Plugins.Plugin.xUri);
    }

    public Collection<ModelContainerFactory> getModelContainerFactories() {
        return Arrays.asList((ModelContainerFactory) new ArtifactModelContainerFactory());
    }

    private static boolean hasExtension(ModelContainer container) {
        for (ModelProperty mp : container.getProperties()) {
            if (mp.getUri().equals(ProjectUri.Build.Plugins.Plugin.extensions) && mp.getValue().equals("true")) {
                return true;
            }
        }
        return false;
    }

    private static class Plugin {

        private String groupId;

        private String artifactId;

        private String version;

        Plugin(List<ModelProperty> modelProperties) {
            for(ModelProperty mp : modelProperties) {
                if(mp.getUri().equals(ProjectUri.Build.Plugins.Plugin.groupId)) {
                    groupId = mp.getValue();
                } else if(mp.getUri().equals(ProjectUri.Build.Plugins.Plugin.artifactId)) {
                    artifactId = mp.getValue();
                } else if(mp.getUri().equals(ProjectUri.Build.Plugins.Plugin.version)) {
                    version = mp.getValue();
                }
                //Add additional info if needed
            }
        }
    }
}
