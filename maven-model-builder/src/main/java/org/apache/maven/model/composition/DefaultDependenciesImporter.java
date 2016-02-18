package org.apache.maven.model.composition;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles the import of dependencies from other models into the target model.
 *
 * @author Christian Schulte
 */
@Component( role = DependenciesImporter.class )
public class DefaultDependenciesImporter
    implements DependenciesImporter
{

    @Override
    public void importDependencies( final Model target, final List<? extends List<? extends Dependency>> sources,
                                    final ModelBuildingRequest request, final ModelProblemCollector problems )
    {
        if ( sources != null && !sources.isEmpty() )
        {
            final Map<String, Dependency> targetDependencies = new LinkedHashMap<>();

            for ( final Dependency targetDependency : target.getDependencies() )
            {
                targetDependencies.put( targetDependency.getManagementKey(), targetDependency );
            }

            final List<Dependency> sourceDependencies = new ArrayList<>( 128 );

            for ( final List<? extends Dependency> source : sources )
            {
                for ( final Dependency sourceDependency : source )
                {
                    if ( !targetDependencies.containsKey( sourceDependency.getManagementKey() ) )
                    {
                        // Intentionally does not check for conflicts in the source dependencies. We want
                        // such conflicts to be resolved manually instead of silently getting dropped.
                        sourceDependencies.add( sourceDependency );
                    }
                }
            }

            final List<Dependency> dependencies = new ArrayList<>( targetDependencies.values() );
            dependencies.addAll( sourceDependencies );

            target.getDependencies().clear();
            target.getDependencies().addAll( dependencies );
        }
    }

}
