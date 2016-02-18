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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
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
            final DependencyManagement targetDependencyManagement = target.getDependencyManagement() != null
                                                                        ? target.getDependencyManagement()
                                                                        : new DependencyManagement();

            target.setDependencyManagement( targetDependencyManagement );

            for ( final Dependency targetDependency : targetDependencyManagement.getDependencies() )
            {
                targetDependencies.put( targetDependency.getManagementKey(), targetDependency );
            }

            final Map<String, List<Dependency>> sourceDependencies = new LinkedHashMap<>();

            for ( final DependencyManagement source : sources )
            {
                for ( final Dependency sourceDependency : source.getDependencies() )
                {
                    if ( !targetDependencies.containsKey( sourceDependency.getManagementKey() ) )
                    {
                        List<Dependency> conflictCanditates =
                            sourceDependencies.get( sourceDependency.getManagementKey() );

                        if ( conflictCanditates == null )
                        {
                            conflictCanditates = new ArrayList<>( source.getDependencies().size() );
                            sourceDependencies.put( sourceDependency.getManagementKey(), conflictCanditates );
                        }

                        conflictCanditates.add( sourceDependency );
                    }
                }
            }

            for ( final List<Dependency> conflictCanditates : sourceDependencies.values() )
            {
                final List<Dependency> conflictingDependencies = removeRedundantDependencies( conflictCanditates );

                // First declaration wins. This is what makes the conflict resolution indeterministic because this
                // solely relies on the order of declaration. There is no such thing as the "first" declaration.
                targetDependencyManagement.getDependencies().add( conflictingDependencies.get( 0 ) );

                // As of Maven 3.6, we print a warning about such conflicting imports using validation level Maven 3.1.
                if ( conflictingDependencies.size() > 1 )
                {
                    final StringBuilder conflictsBuilder = new StringBuilder( conflictingDependencies.size() * 128 );

                    for ( final Dependency dependency : conflictingDependencies )
                    {
                        final InputLocation location = dependency.getLocation( "" );

                        if ( location != null )
                        {
                            final InputSource inputSource = location.getSource();

                            if ( inputSource != null )
                            {
                                conflictsBuilder.append( ", '" ).append( inputSource.getModelId() ).append( '\'' );
                            }
                        }
                    }

                    problems.add( new ModelProblemCollectorRequest(
                        effectiveSeverity( request.getValidationLevel(),
                                           ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1 ),
                        ModelProblem.Version.V20 ).
                        setMessage( String.format(
                            "Dependency '%1$s' has conflicting dependency management in model '%2$s'%3$s%4$s. "
                                + "To resolve the conflicts, declare the dependency management for dependency '%1$s' "
                                + "directly in the dependency management of model '%2$s' to override what gets "
                                + "imported. If the Maven version in use supports it, add exclusions for the "
                                + "conflicting dependencies or use the include scope feature to rearrange the "
                                + "causing dependencies in the inheritance hierarchy applying standard override logic "
                                + "based on artifact coordinates. Without resolving the conflicts, your build relies "
                                + "on indeterministic behaviour.",
                            conflictingDependencies.get( 0 ).getManagementKey(), target.getId(),
                            target.getPomFile() != null
                                ? " @ '" + target.getPomFile().getAbsolutePath() + "' "
                                : " ", conflictsBuilder.length() > 0
                                           ? "(" + conflictsBuilder.substring( 2 ) + ")"
                                           : "" ) ) );

                }
            }
        }
    }

    private static List<Dependency> removeRedundantDependencies( final List<Dependency> candidateDependencies )
    {
        final List<Dependency> resultDependencies = new ArrayList<>( candidateDependencies.size() );

        while ( !candidateDependencies.isEmpty() )
        {
            final Dependency resultDependency = candidateDependencies.remove( 0 );
            resultDependencies.add( resultDependency );

            // Removes redundant dependencies.
            for ( final Iterator<Dependency> it = candidateDependencies.iterator(); it.hasNext(); )
            {
                final Dependency candidateDependency = it.next();
                boolean redundant = true;

                redundancy_check:
                {
                    if ( !( resultDependency.getOptional() != null
                            ? resultDependency.getOptional().equals( candidateDependency.getOptional() )
                            : candidateDependency.getOptional() == null ) )
                    {
                        redundant = false;
                        break redundancy_check;
                    }

                    if ( !( effectiveScope( resultDependency ).equals( effectiveScope( candidateDependency ) ) ) )
                    {
                        redundant = false;
                        break redundancy_check;
                    }

                    if ( !( resultDependency.getSystemPath() != null
                            ? resultDependency.getSystemPath().equals( candidateDependency.getSystemPath() )
                            : candidateDependency.getSystemPath() == null ) )
                    {
                        redundant = false;
                        break redundancy_check;
                    }

                    if ( !( resultDependency.getVersion() != null
                            ? resultDependency.getVersion().equals( candidateDependency.getVersion() )
                            : candidateDependency.getVersion() == null ) )
                    {
                        redundant = false;
                        break redundancy_check;
                    }

                    for ( int i = 0, s0 = resultDependency.getExclusions().size(); i < s0; i++ )
                    {
                        final Exclusion resultExclusion = resultDependency.getExclusions().get( i );

                        if ( !containsExclusion( candidateDependency.getExclusions(), resultExclusion ) )
                        {
                            redundant = false;
                            break redundancy_check;
                        }
                    }

                    for ( int i = 0, s0 = candidateDependency.getExclusions().size(); i < s0; i++ )
                    {
                        final Exclusion candidateExclusion = candidateDependency.getExclusions().get( i );

                        if ( !containsExclusion( resultDependency.getExclusions(), candidateExclusion ) )
                        {
                            redundant = false;
                            break redundancy_check;
                        }
                    }
                }

                if ( redundant )
                {
                    it.remove();
                }
            }
        }

        return resultDependencies;
    }

    private static boolean containsExclusion( final List<Exclusion> exclusions, final Exclusion exclusion )
    {
        for ( int i = 0, s0 = exclusions.size(); i < s0; i++ )
        {
            final Exclusion current = exclusions.get( i );

            if ( ( exclusion.getArtifactId() != null
                   ? exclusion.getArtifactId().equals( current.getArtifactId() )
                   : current.getArtifactId() == null )
                     && ( exclusion.getGroupId() != null
                          ? exclusion.getGroupId().equals( current.getGroupId() )
                          : current.getGroupId() == null ) )
            {
                return true;
            }
        }

        return false;
    }

    private static String effectiveScope( final Dependency dependency )
    {
        return dependency.getScope() == null
                   ? "compile"
                   : dependency.getScope();

    }

    private static ModelProblem.Severity effectiveSeverity( final int validationLevel, final int errorThreshold )
    {
        if ( validationLevel < errorThreshold )
        {
            return ModelProblem.Severity.WARNING;
        }
        else
        {
            return ModelProblem.Severity.ERROR;
        }
    }

}
