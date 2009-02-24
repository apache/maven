package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.JoinRule;
import org.apache.maven.project.builder.factories.ArtifactModelContainerFactory;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.List;
import java.util.ArrayList;

/**
 * 
 */
public class OverideConfigTransformerRule implements JoinRule
{
    public List<ModelProperty> execute(List<ModelProperty> modelProperties) throws DataSourceException
    {
        ModelDataSource source = new DefaultModelDataSource( modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );
        List<ModelContainer> reportContainers = source.queryFor( ProjectUri.Reporting.Plugins.Plugin.xUri );
        for ( ModelContainer pluginContainer : source.queryFor( ProjectUri.Build.Plugins.Plugin.xUri ) )
        {
            ModelContainer transformedReportContainer = new ArtifactModelContainerFactory().create(
                    transformPlugin( pluginContainer.getProperties() ) );

            for(ModelContainer reportContainer : reportContainers) {
                ModelContainerAction action = transformedReportContainer.containerAction( reportContainer );
                if ( action.equals( ModelContainerAction.JOIN ) )
                {
                    source.join( transformedReportContainer, reportContainer );
                    break;
                }
            }
        }

        return source.getModelProperties();
    }

    private static List<ModelProperty> transformPlugin( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> transformedProperties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.Build.Plugins.xUri ) )
            {   if(mp.getUri().startsWith(ProjectUri.Build.Plugins.Plugin.configuration)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.groupId)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.artifactId)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.version)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.xUri ) )
                {
                transformedProperties.add( new ModelProperty(
                    mp.getUri().replace( ProjectUri.Build.Plugins.xUri, ProjectUri.Reporting.Plugins.xUri ),
                    mp.getResolvedValue() ) );
                }

            }
        }
        return transformedProperties;
    }    
}
