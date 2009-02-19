package org.apache.maven.project.builder.factories;

import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.project.builder.ProjectUri;

import java.util.*;

public class ExclusionModelContainerFactory implements ModelContainerFactory
{

    private static final Collection<String> uris = Collections.unmodifiableList( Arrays.asList(

        ProjectUri.Dependencies.Dependency.Exclusions.Exclusion.xUri

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
        return new ExclusionModelContainer( modelProperties );
    }

    private static class ExclusionModelContainer
        implements ModelContainer
    {

        public ExclusionModelContainer(List<ModelProperty> properties) {
            this.properties = properties;
        }

        private List<ModelProperty> properties;


        public ModelContainerAction containerAction( ModelContainer modelContainer )
        {
            throw new UnsupportedOperationException();
        }

        public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
        {
            return new ExclusionModelContainer( modelProperties );
        }

        public List<ModelProperty> getProperties()
        {
            return properties;
        }

    }
}

