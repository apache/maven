package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.TransformerRemovalRule;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.List;
import java.util.ArrayList;

/**
 * If plugin inherited element value is false, do not inherit the plugin.
 */
public class NotInheritedPluginTransformerRule implements TransformerRemovalRule
{
    public List<ModelProperty> executeWithReturnPropertiesToRemove(List<ModelProperty> modelProperties, boolean isMostSpecialized)
            throws DataSourceException
    {
        List<ModelProperty> removeProperties = new ArrayList<ModelProperty>();
        if ( !isMostSpecialized)
        {
            ModelDataSource source = new DefaultModelDataSource( modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );
            List<ModelContainer> containers = source.queryFor( ProjectUri.Build.Plugins.Plugin.xUri );
            for ( ModelContainer container : containers )
            {
                for ( ModelProperty mp : container.getProperties() )
                {
                    if ( mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.inherited ) && mp.getResolvedValue() != null &&
                        mp.getResolvedValue().equals( "false" ) )
                    {
                        removeProperties.addAll( container.getProperties() );
                        for ( int j = modelProperties.indexOf( mp ); j >= 0; j-- )
                        {
                            if ( modelProperties.get( j ).getUri().equals( ProjectUri.Build.Plugins.Plugin.xUri ) )
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
