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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles the import of dependency management from other models into the target model.
 *
 * @author Benjamin Bentmann
 */
@Component( role = DependencyManagementImporter.class )
public class DefaultDependencyManagementImporter
    implements DependencyManagementImporter
{

    @Override
    public void importManagement( final Model target, final List<? extends DependencyManagement> sources,
                                  final ModelBuildingRequest request, final ModelProblemCollector problems )
    {
        if ( sources != null && !sources.isEmpty() )
        {
            final Map<String, Dependency> targetDependencies = new LinkedHashMap<>();

            if ( target.getDependencyManagement() != null )
            {
                for ( final Dependency targetDependency : target.getDependencyManagement().getDependencies() )
                {
                    targetDependencies.put( targetDependency.getManagementKey(), targetDependency );
                }
            }

            for ( final DependencyManagement source : sources )
            {
                for ( final Dependency sourceDependency : source.getDependencies() )
                {
                    if ( !targetDependencies.containsKey( sourceDependency.getManagementKey() ) )
                    {
                        targetDependencies.put( sourceDependency.getManagementKey(), sourceDependency );
                    }
                }
            }

            target.setDependencyManagement( new DependencyManagement() );
            target.getDependencyManagement().getDependencies().addAll( targetDependencies.values() );
        }
    }

}
