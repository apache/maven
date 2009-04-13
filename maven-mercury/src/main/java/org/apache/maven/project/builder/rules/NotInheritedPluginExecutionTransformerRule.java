package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.factories.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.factories.PluginExecutionIdModelContainerFactory;
import org.apache.maven.project.builder.legacy.TransformerRemovalRule;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * If plugin execution inherited property is false, do not inherit the execution
 */
public class NotInheritedPluginExecutionTransformerRule implements TransformerRemovalRule
{
    public List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, boolean isMostSpecialized)
            throws DataSourceException
    {
        List<ModelProperty> removeProperties = new ArrayList<ModelProperty>();

        if ( !isMostSpecialized)
        {
            ModelDataSource source = new DefaultModelDataSource( modelProperties, Arrays.asList(
                    new ArtifactModelContainerFactory(), new PluginExecutionIdModelContainerFactory() ));
            List<ModelContainer> containers =
                    source.queryFor( ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri );
            for ( ModelContainer container : containers )
            {
                for ( ModelProperty mp : container.getProperties() )
                {
                    if ( mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.Execution.inherited ) &&
                            mp.getResolvedValue() != null && mp.getResolvedValue().equals( "false" ) )
                    {
                        removeProperties.addAll( container.getProperties() );
                        for ( int j = modelProperties.indexOf( mp ); j >= 0; j-- )
                        {
                            if ( modelProperties.get( j ).getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.xUri ) )
                            {
                                removeProperties.add( modelProperties.get( j ) );
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return removeProperties;
    }
}
