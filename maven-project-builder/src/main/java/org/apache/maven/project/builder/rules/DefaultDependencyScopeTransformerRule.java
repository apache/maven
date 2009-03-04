package org.apache.maven.project.builder.rules;

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.apache.maven.project.builder.TransformerRule;
import org.apache.maven.project.builder.factories.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;
import java.util.Arrays;


/**
 * If no scope is found in most specialized model, then set scope to compile.
 */
public class DefaultDependencyScopeTransformerRule implements TransformerRule
{
    public void execute(List<ModelProperty> modelProperties, boolean isMostSpecialized)
        throws DataSourceException
    {
        if(isMostSpecialized)
        {
            ModelDataSource s = new DefaultModelDataSource( modelProperties, Arrays.asList( new ArtifactModelContainerFactory()) );
            for(ModelContainer mc : s.queryFor(ProjectUri.Dependencies.Dependency.xUri))
            {
                boolean containsScope = false;
                for(ModelProperty mp :mc.getProperties())
                {
                    if(mp.getUri().equals(ProjectUri.Dependencies.Dependency.scope)) {
                        containsScope = true;
                        break;
                    }
                }

                if(!containsScope)
                {
                    modelProperties.add(modelProperties.indexOf(mc.getProperties().get(0)) + 1,
                            new ModelProperty(ProjectUri.Dependencies.Dependency.scope, "compile"));
                }
            }
        }
    }
}
