package org.apache.maven.project.builder.rules;

import org.apache.maven.project.builder.DataSourceRule;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.project.builder.factories.ArtifactModelContainerFactory;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Transform Dependency Management section of pom into dependency section
 */
public class DependencyManagementDataSourceRule implements DataSourceRule 
{
    public void execute(ModelDataSource source) throws DataSourceException
    {
        for ( ModelContainer dependencyContainer : source.queryFor( ProjectUri.Dependencies.Dependency.xUri ) )
        {
            for ( ModelContainer managementContainer : source.queryFor(
                ProjectUri.DependencyManagement.Dependencies.Dependency.xUri ) )
            {
                //Join Duplicate Exclusions TransformerRule (MNG-4010)
                ModelDataSource exclusionSource = new DefaultModelDataSource(managementContainer.getProperties(),
                        Collections.unmodifiableList(Arrays.asList(new ArtifactModelContainerFactory(ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.Exclusion.xUri))));
                List<ModelContainer> exclusionContainers =
                        exclusionSource.queryFor(ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.Exclusion.xUri);

                for(ModelContainer mc : exclusionContainers)
                {
                    for(ModelContainer mc1 : exclusionContainers)
                    {
                        if(!mc.equals(mc1)  && mc.containerAction(mc1).equals(ModelContainerAction.JOIN))
                        {
                            exclusionSource.joinWithOriginalOrder(mc1, mc);
                        }
                    }
                }

                managementContainer = new ArtifactModelContainerFactory().create(
                    transformDependencyManagement( exclusionSource.getModelProperties() ) );
                ModelContainerAction action = dependencyContainer.containerAction( managementContainer );
                if ( action.equals( ModelContainerAction.JOIN ) || action.equals( ModelContainerAction.DELETE ) )
                {
                    source.join( dependencyContainer, managementContainer );
                }
            }
        }
    }

    private static List<ModelProperty> transformDependencyManagement( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> transformedProperties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.DependencyManagement.xUri ) )
            {
                transformedProperties.add( new ModelProperty(
                    mp.getUri().replace( ProjectUri.DependencyManagement.xUri, ProjectUri.xUri ), mp.getResolvedValue() ) );
            }
        }
        return transformedProperties;
    }
}
