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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Handles dependency management graph created from importing dependencies from other POMs.
 * 
 * @author Michal Kowalcze
 * @since 3.4.0
 */
public class DependencyManagementGraph
{

    private static final Optional<Integer> DIRECT_DEPENDENCY = Optional.of( 0 );

    /**
     * Stores dependencies directly declared in a dependency management section.
     */
    private Map<DependencyManagement, List<Dependency>> declaredDependenciesStorage = new HashMap<>();

    /**
     * Stores imported dependency managements for a dependency management section.
     */
    private Map<DependencyManagement, List<DependencyManagement>> importedDependencyManagementsStorage =
        new HashMap<>();

    /**
     * Finds depth of a particular dependency inside dependency management hierarchy.
     * 
     * @param dependencyManagement
     * @param dependency
     * @since 3.4.0
     * @return Optional<Integer> - absent if dependency not found, depth value otherwise
     */
    public Optional<Integer> findDeclaredDependencyDepth( DependencyManagement dependencyManagement,
                                                          Dependency dependency )
    {
        List<Dependency> declaredDependencies = getDeclaredDependencies( dependencyManagement );

        if ( declaredDependencies.contains( dependency ) )
        {
            return DIRECT_DEPENDENCY;
        }
        else
        {
            int depth = Integer.MAX_VALUE;
            List<DependencyManagement> importedDependencyManagements =
                getImportedDependencyManagements( dependencyManagement );

            for ( DependencyManagement importedDependencyManagement : importedDependencyManagements )
            {
                Optional<Integer> nodeDepth = findDeclaredDependencyDepth( importedDependencyManagement, dependency );
                if ( nodeDepth.isPresent() )
                {
                    depth = Math.min( nodeDepth.get(), depth );
                }
            }

            boolean dependencyFound = depth != Integer.MAX_VALUE;
            if ( dependencyFound )
            {
                return Optional.of( 1 + depth );
            }
        }

        return Optional.absent();
    }

    public void addDeclaredDependency( DependencyManagement depMgmt, Dependency dependency )
    {
        getDeclaredDependencies( depMgmt ).add( dependency );
    }

    public void addImportedDependencyManagement( DependencyManagement parentDepMgmt,
                                                 DependencyManagement importedDepMgmt )
    {
        getImportedDependencyManagements( parentDepMgmt ).add( importedDepMgmt );
    }

    private List<Dependency> getDeclaredDependencies( DependencyManagement dependencyManagement )
    {
        List<Dependency> declaredDependencies = this.declaredDependenciesStorage.get( dependencyManagement );
        if ( null == declaredDependencies )
        {
            declaredDependencies = Lists.newArrayList();
            this.declaredDependenciesStorage.put( dependencyManagement, declaredDependencies );
        }
        return declaredDependencies;
    }

    private List<DependencyManagement> getImportedDependencyManagements( DependencyManagement dependencyManagement )
    {
        List<DependencyManagement> importedDependencyManagements =
            this.importedDependencyManagementsStorage.get( dependencyManagement );
        if ( null == importedDependencyManagements )
        {
            importedDependencyManagements = Lists.newArrayList();
            this.importedDependencyManagementsStorage.put( dependencyManagement, importedDependencyManagements );
        }
        return importedDependencyManagements;
    }
}
