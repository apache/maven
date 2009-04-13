package org.apache.maven.project.builder.factories;

import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.project.builder.ProjectUri;

import java.util.*;

public class PluginExecutionIdModelContainerFactory implements ModelContainerFactory {

    private static final Collection<String> uris = Collections.unmodifiableList(Arrays.asList(
            ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri,
            ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.Execution.xUri));

    public Collection<String> getUris() {
        return uris;
    }

    public ModelContainer create(List<ModelProperty> modelProperties) {
        if ( modelProperties == null || modelProperties.size() == 0 )
        {
            throw new IllegalArgumentException( "modelProperties: null or empty" );
        }
        return new PluginExecutionIdModelContainer( modelProperties );
    }

    private static class PluginExecutionIdModelContainer
        implements ModelContainer
    {

        private String id;

        private List<ModelProperty> properties;

        private PluginExecutionIdModelContainer( List<ModelProperty> properties )
        {
            this.properties = new ArrayList<ModelProperty>( properties );
            this.properties = Collections.unmodifiableList( this.properties );

            for ( ModelProperty mp : properties )
            {
                if ( mp.getUri().endsWith( "/id" ) )
                {
                    this.id = mp.getResolvedValue();
                }
            }
        }

        public ModelContainerAction containerAction( ModelContainer modelContainer )
        {
            if ( modelContainer == null )
            {
                throw new IllegalArgumentException( "modelContainer: null" );
            }

            if ( !( modelContainer instanceof PluginExecutionIdModelContainer ) )
            {
                throw new IllegalArgumentException( "modelContainer: wrong type" );
            }

            PluginExecutionIdModelContainer c = (PluginExecutionIdModelContainer) modelContainer;
            if ( c.id == null || id == null )
            {
                return ModelContainerAction.NOP;
            }
            return ( c.id.equals( id ) ) ? ModelContainerAction.JOIN : ModelContainerAction.NOP;
        }

        public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
        {
            return new PluginExecutionIdModelContainer( modelProperties );
        }

        public List<ModelProperty> getProperties()
        {
            return properties;
        }

        public String toString()
        {
            return "ID = " + id;
        }
    }
}
