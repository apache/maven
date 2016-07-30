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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * A {@code ModelFinalizer} adding classified dependency declarations to the dependency management for all unclassified
 * dependency declarations with classifiers taken from a collection of well-known classifiers.
 * <p>
 * This {@code ModelFinalizer} implementation is not wired by default. It will need to be setup manually. This can be
 * done by adding a {@code META-INF/plexus/components.xml} file containing the following to the Maven core classpath,
 * for example.
 * <pre>
 * &lt;component-set>
 *   &lt;components>
 *     &lt;component>
 *       &lt;role>org.apache.maven.model.finalization.ModelFinalizer&lt;/role>
 *       &lt;implementation>org.apache.maven.model.finalization.DependencyManagementModelFinalizer&lt;/implementation>
 *       &lt;role-hint>dependency-management&lt;/role-hint>
 *       &lt;requirements>
 *         &lt;requirement>
 *           &lt;role>org.apache.maven.model.management.DependencyManagementInjector</role>
 *           &lt;field-name>dependencyManagementInjector</field-name>
 *         &lt;/requirement>
 *       &lt;/requirements>
 *       &lt;configuration>
 *         &lt;dependencyManagementClassifiers>
 *           &lt;dependencyManagementClassifier>tests&lt;/dependencyManagementClassifier>
 *           &lt;dependencyManagementClassifier>javadoc&lt;/dependencyManagementClassifier>
 *           &lt;dependencyManagementClassifier>test-javadoc&lt;/dependencyManagementClassifier>
 *           &lt;dependencyManagementClassifier>shaded&lt;/dependencyManagementClassifier>
 *           &lt;dependencyManagementClassifier>site&lt;/dependencyManagementClassifier>
 *           &lt;dependencyManagementClassifier>sources&lt;/dependencyManagementClassifier>
 *           &lt;dependencyManagementClassifier>test-sources&lt;/dependencyManagementClassifier>
 *         &lt;/dependencyManagementClassifiers>
 *       &lt;/configuration>
 *     &lt;/component>
 *   &lt;/components>
 * &lt;/component-set>
 * </pre>
 * </p>
 *
 * @author Christian Schulte
 * @since 3.4
 */
public final class DependencyManagementModelFinalizer
    implements ModelFinalizer
{

    @Requirement
    private DependencyManagementInjector dependencyManagementInjector;

    private Collection<String> dependencyManagementClassifiers;

    public DependencyManagementModelFinalizer setDependencyManagementInjector( DependencyManagementInjector value )
    {
        this.dependencyManagementInjector = value;
        return this;
    }

    /**
     * Gets the collection of well-known classifiers to use for adding classified dependency declarations.
     *
     * @return The collection of well-known classifiers to use.
     */
    public Collection<String> getDependencyManagementClassifiers()
    {
        if ( this.dependencyManagementClassifiers == null )
        {
            this.dependencyManagementClassifiers = new HashSet<>();
        }

        return this.dependencyManagementClassifiers;
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
        if ( model.getDependencyManagement() != null )
        {
            final Set<Dependency> classifiedDependencies = new HashSet<>();

            for ( final Dependency managedDependency : model.getDependencyManagement().getDependencies() )
            {
                for ( final String classifier : this.getDependencyManagementClassifiers() )
                {
                    Dependency classifiedDependency =
                        getDependency( model.getDependencyManagement(), managedDependency.getGroupId(),
                                       managedDependency.getArtifactId(), managedDependency.getType(),
                                       classifier );

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
