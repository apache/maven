package org.apache.maven.project.builder.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelEventListener;
import org.apache.maven.shared.model.ModelProperty;

public class BuildExtensionListener
    implements ModelEventListener
{
    private boolean inBuild;

    public BuildExtensionListener(boolean inBuild)
    {
        this.inBuild = inBuild;
    }

    public void fire( List<ModelContainer> modelContainers )
    {
        if(!inBuild) {
            return;
        }
        
        List<BuildExtension> buildExtensions = new ArrayList<BuildExtension>();
        for ( ModelContainer mc : modelContainers )
        {
            if ( hasExtension( mc ) )
            {
                buildExtensions.add( new BuildExtension( mc.getProperties() ) );
            }
        }

        for( BuildExtension be : buildExtensions )
        {
            System.out.println( "Extension ---> " + be.groupId + " : " + be.artifactId + " : " + be.version );
        }        
    }

    public List<String> getUris()
    {
        return Arrays.asList( ProjectUri.Build.Extensions.Extension.xUri );
    }

    public Collection<ModelContainerFactory> getModelContainerFactories()
    {
        return Arrays.asList( (ModelContainerFactory) new ArtifactModelContainerFactory() );
    }

    private static boolean hasExtension( ModelContainer container )
    {
        for ( ModelProperty mp : container.getProperties() )
        {
            if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.xUri ) )
            {
                return true;
            }
        }
        return false;
    }

    private static class BuildExtension
    {
        private String groupId;

        private String artifactId;

        private String version;

        BuildExtension( List<ModelProperty> modelProperties )
        {
            for ( ModelProperty mp : modelProperties )
            {
                if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.groupId ) )
                {
                    groupId = mp.getValue();
                }
                else if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.artifactId ) )
                {
                    artifactId = mp.getValue();
                }
                else if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.version ) )
                {
                    version = mp.getValue();
                }
            }
        }        
    }
}
