package org.apache.maven.project.builder.factories;

import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.project.builder.ProjectUri;

import java.util.*;

public class AlwaysJoinModelContainerFactory
    implements ModelContainerFactory
{

    private static final Collection<String> uris = Collections.unmodifiableList( Arrays.asList(

        ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.goal
     //   ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri

         ) );

    public Collection<String> getUris()
    {
        return uris;
    }

    public ModelContainer create( List<ModelProperty> modelProperties )
    {
        if ( modelProperties == null || modelProperties.size() == 0 )
        {
            throw new IllegalArgumentException( "modelProperties: null or empty" );
        }
        return new Anon_ModelContainer( modelProperties );
    }

    private static class Anon_ModelContainer
        implements ModelContainer
    {

        public Anon_ModelContainer(List<ModelProperty> properties) {
            this.properties = new ArrayList<ModelProperty>(properties);
        }

        private List<ModelProperty> properties;


        public ModelContainerAction containerAction( ModelContainer modelContainer )
        {
            return ModelContainerAction.JOIN;
        }

        public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
        {
            return new Anon_ModelContainer( modelProperties );
        }

        public List<ModelProperty> getProperties()
        {
            return new ArrayList<ModelProperty>(properties);
        }

    }
}
