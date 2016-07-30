package org.apache.maven.model.finalization;

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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * A {@code ModelFinalizer} adding classified dependency declarations to the dependency management for all unclassified
 * dependency declarations with classifiers taken from set of well-known classifiers.
 * <p>
 * This class requires the {@code Set} implementation of well-known classifiers to be provided by the runtime
 * environment. By default, no such implementation exists. As a starting point, that {@code Set} implementation should
 * at least contain the following classifiers corresponding to plugin defaults.
 * <table>
 * <tr>
 * <th>Plugin</th>
 * <th>Classifiers</th>
 * </tr>
 * <tr>
 * <td>maven-jar-plugin</td>
 * <td>tests</td>
 * </tr>
 * <tr>
 * <td>maven-javadoc-plugin</td>
 * <td>javadoc, test-javadoc</td>
 * </tr>
 * <tr>
 * <td>maven-shade-plugin</td>
 * <td>shaded</td>
 * </tr>
 * <tr>
 * <td>maven-site-plugin</td>
 * <td>site</td>
 * </tr>
 * <tr>
 * <td>maven-source-plugin</td>
 * <td>sources, test-sources</td>
 * </tr>
 * </table>
 * </p>
 *
 * @author Christian Schulte
 * @since 3.4
 */
@Component( role = ModelFinalizer.class, hint = "dependency-management" )
public class DependencyManagementModelFinalizer
    implements ModelFinalizer
{

    @Requirement( role = Set.class, hint = "dependency-management-classifiers", optional = true )
    private Set<String> dependencyManagementClassifiers;

    @Requirement
    private DependencyManagementInjector dependencyManagementInjector;

    public DependencyManagementModelFinalizer setDependencyManagementInjector( DependencyManagementInjector value )
    {
        this.dependencyManagementInjector = value;
        return this;
    }

    public DependencyManagementModelFinalizer setDependencyManagementClassifiers( Set<String> value )
    {
        this.dependencyManagementClassifiers = value;
        return this;
    }

    /**
     * Adds classified dependency declarations for all unclassified dependency declarations to the dependency management
     * of the given {@code Model} with classifiers taken from set of well-known classifiers.
     *
     * @param model The {@code Model} to add classified dependency declarations to, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    @Override
    public void finalizeModel( final Model model, final ModelBuildingRequest request,
                               final ModelProblemCollector problems )
    {
        if ( this.dependencyManagementClassifiers != null && model.getDependencyManagement() != null )
        {
            final Set<Dependency> classifiedDependencies = new HashSet<>();

            for ( final Dependency managedDependency : model.getDependencyManagement().getDependencies() )
            {
                for ( final String classifier : this.dependencyManagementClassifiers )
                {
                    Dependency classifiedDependency =
                        getDependency( model.getDependencyManagement(), managedDependency.getGroupId(),
                                       managedDependency.getArtifactId(), managedDependency.getType(),
                                       managedDependency.getClassifier() );

                    if ( classifiedDependency == null )
                    {
                        classifiedDependency = managedDependency.clone();
                        classifiedDependencies.add( classifiedDependency );

                        classifiedDependency.setClassifier( classifier );
                    }
                }
            }

            if ( !classifiedDependencies.isEmpty() )
            {
                model.getDependencyManagement().getDependencies().addAll( classifiedDependencies );
                this.dependencyManagementInjector.injectManagement( model, request, problems );
            }
        }
    }

    private static Dependency getDependency( final DependencyManagement dependencyManagement,
                                             final String groupId, final String artifactId, final String type,
                                             final String classifier )
    {
        Dependency dependency = null;

        for ( final Dependency candidate : dependencyManagement.getDependencies() )
        {
            if ( groupId.equals( candidate.getGroupId() )
                     && artifactId.equals( candidate.getArtifactId() )
                     && type.equals( candidate.getType() )
                     && classifier.equals( candidate.getClassifier() ) )
            {
                dependency = candidate;
                break;
            }
        }

        return dependency;
    }

}
