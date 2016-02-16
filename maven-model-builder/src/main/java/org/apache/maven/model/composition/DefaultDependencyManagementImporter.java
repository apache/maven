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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.codehaus.plexus.component.annotations.Component;

import com.google.common.base.Optional;

/**
 * Handles the import of dependency management from other models into the target model.
 *
 * @author Benjamin Bentmann
 */
@Component( role = DependencyManagementImporter.class )
public class DefaultDependencyManagementImporter
    implements DependencyManagementImporter
{
    private static final String PROPERTY_NEAREST_MATCH_ENABLED =
        "org.apache.maven.model.composition.DependencyManagementImporter.nearestMatchEnabled";

    @Override
    public void importManagement( Model target, List<? extends DependencyManagement> sources,
                                  ModelBuildingRequest request, ModelProblemCollector problems,
                                  DependencyManagementGraph dependencyManagementGraph )
    {
        if ( sources != null && !sources.isEmpty() )
        {
            Map<String, Dependency> dependencies = new LinkedHashMap<>();
            Set<String> directDependencies = new HashSet<>();
            Map<String, DependencyManagement> dependencySources = new LinkedHashMap<>();

            DependencyManagement depMngt = target.getDependencyManagement();

            if ( depMngt != null )
            {
                for ( Dependency dependency : depMngt.getDependencies() )
                {
                    dependencies.put( dependency.getManagementKey(), dependency );
                    directDependencies.add( dependency.getManagementKey() );
                }
            }
            else
            {
                depMngt = new DependencyManagement();
                target.setDependencyManagement( depMngt );
            }

            for ( DependencyManagement source : sources )
            {
                for ( Dependency dependency : source.getDependencies() )
                {
                    String key = dependency.getManagementKey();
                    if ( !dependencies.containsKey( key ) )
                    {
                        storeDependency( dependencies, dependencySources, source, dependency, key );
                    }
                    else if ( nearestMatchEnabled( target ) && !directDependencies.contains( key ) )
                    {
                        // non-direct dependency - check source depths to determine distance
                        DependencyManagement sourceDepMngt = dependencySources.get( key );
                        Dependency sourceDependency = dependencies.get( key );

                        Optional<Integer> previousSourceDepth =
                            dependencyManagementGraph.findDeclaredDependencyDepth( sourceDepMngt, sourceDependency );
                        if ( previousSourceDepth.isPresent() )
                        {
                            Optional<Integer> currentDepth =
                                dependencyManagementGraph.findDeclaredDependencyDepth( source, dependency );
                            if ( currentDepth.isPresent() )
                            {
                                boolean currentDependencyNearest = currentDepth.get() < previousSourceDepth.get();

                                if ( currentDependencyNearest )
                                {
                                    storeDependency( dependencies, dependencySources, source, dependency, key );
                                }
                            }
                            else
                            {
                                String message = "[MNG-5947] Invalid state - current source depth not found for "
                                    + dependency.getManagementKey() + " in " + describeTarget( target );
                                addProblem( problems, dependency, message );
                            }
                        }
                        else
                        {
                            String msg = "[MNG-5947] Invalid state - previous source depth not found for "
                                + dependency.getManagementKey() + " in " + describeTarget( target );
                            addProblem( problems, dependency, msg );
                        }
                    }
                }
            }

            depMngt.setDependencies( new ArrayList<>( dependencies.values() ) );
        }
    }

    void storeDependency( Map<String, Dependency> dependencies, Map<String, DependencyManagement> dependencySources,
                          DependencyManagement source, Dependency dependency, String key )
    {
        dependencies.put( key, dependency );
        dependencySources.put( key, source );
    }

    private boolean nearestMatchEnabled( Model target )
    {
        String propValue = target.getProperties().getProperty( PROPERTY_NEAREST_MATCH_ENABLED );
        return Boolean.parseBoolean( propValue );
    }

    private void addProblem( ModelProblemCollector problems, Dependency dependency, String message )
    {
        problems.add( //
                      new ModelProblemCollectorRequest( Severity.WARNING, Version.BASE )//
                      .setMessage( message )//
                      .setLocation( dependency.getLocation( "" ) ) );
    }

    private String describeTarget( Model target )
    {
        return target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion();
    }

}
