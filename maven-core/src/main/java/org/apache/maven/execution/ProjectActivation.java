package org.apache.maven.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Container for storing the request from the user to activate or deactivate certain projects and optionally fail the
 * build if those projects do not exist.
 */
public class ProjectActivation
{
    private static class ProjectActivationSettings
    {
        /**
         * The selector of a project. This can be the project directory, [groupId]:[artifactId] or :[artifactId].
         */
        final String selector;

        /**
         * This describes how/when to active or deactivate the project.
         */
        final ActivationSettings activationSettings;

        ProjectActivationSettings( String selector, ActivationSettings activationSettings )
        {
            this.selector = selector;
            this.activationSettings = activationSettings;
        }
    }

    /**
     * List of activated and deactivated projects.
     */
    private final List<ProjectActivationSettings> activations = new ArrayList<>();

    /**
     * Adds a project activation to the request.
     * @param selector The selector of the project.
     * @param active Should the project be activated?
     * @param optional Can the build continue if the project does not exist?
     */
    public void addProjectActivation( String selector, boolean active, boolean optional )
    {
        final ActivationSettings settings = ActivationSettings.of( active, optional );
        this.activations.add( new ProjectActivationSettings( selector, settings ) );
    }

    private Stream<ProjectActivationSettings> getProjects( final Predicate<ActivationSettings> predicate )
    {
        return this.activations.stream()
                .filter( activation -> predicate.test( activation.activationSettings ) );
    }

    private Set<String> getProjectSelectors( final Predicate<ActivationSettings> predicate )
    {
        return getProjects( predicate )
                .map( activation -> activation.selector )
                .collect( toSet() );
    }

    /**
     * @return Required active project selectors, never {@code null}.
     */
    public Set<String> getRequiredActiveProjectSelectors()
    {
        return getProjectSelectors( pa -> !pa.optional && pa.active );
    }

    /**
     * @return Optional active project selectors, never {@code null}.
     */
    public Set<String> getOptionalActiveProjectSelectors()
    {
        return getProjectSelectors( pa -> pa.optional && pa.active );
    }

    /**
     * @return Required inactive project selectors, never {@code null}.
     */
    public Set<String> getRequiredInactiveProjectSelectors()
    {
        return getProjectSelectors( pa -> !pa.optional && !pa.active );
    }

    /**
     * @return Optional inactive project selectors, never {@code null}.
     */
    public Set<String> getOptionalInactiveProjectSelectors()
    {
        return getProjectSelectors( pa -> pa.optional && !pa.active );
    }

    /**
     * Mimics the pre-Maven 4 "selected projects" list.
     * @deprecated Use {@link #getRequiredActiveProjectSelectors()} and {@link #getOptionalActiveProjectSelectors()}
     * instead.
     */
    @Deprecated
    public List<String> getSelectedProjects()
    {
        return Collections.unmodifiableList( new ArrayList<>( getProjectSelectors( pa -> pa.active ) ) );
    }

    /**
     * Mimics the pre-Maven 4 "excluded projects" list.
     * @deprecated Use {@link #getRequiredInactiveProjectSelectors()} and {@link #getOptionalInactiveProjectSelectors()}
     * instead.
     */
    @Deprecated
    public List<String> getExcludedProjects()
    {
        return Collections.unmodifiableList( new ArrayList<>( getProjectSelectors( pa -> !pa.active ) ) );
    }

    /**
     * Overwrites the active projects based on a pre-Maven 4 "active projects" list.
     * @param activeProjectSelectors A {@link List} of project selectors that must be activated.
     * @deprecated Use {@link #activateOptionalProject(String)} or {@link #activateRequiredProject(String)} instead.
     */
    @Deprecated
    public void overwriteActiveProjects( List<String> activeProjectSelectors )
    {
        List<ProjectActivationSettings> projects = getProjects( pa -> pa.active ).collect( Collectors.toList() );
        this.activations.removeAll( projects );
        activeProjectSelectors.forEach( this::activateOptionalProject );
    }

    /**
     * Overwrites the inactive projects based on a pre-Maven 4 "inactive projects" list.
     * @param inactiveProjectSelectors A {@link List} of project selectors that must be deactivated.
     * @deprecated Use {@link #deactivateOptionalProject(String)} or {@link #deactivateRequiredProject(String)} instead.
     */
    @Deprecated
    public void overwriteInactiveProjects( List<String> inactiveProjectSelectors )
    {
        List<ProjectActivationSettings> projects = getProjects( pa -> !pa.active ).collect( Collectors.toList() );
        this.activations.removeAll( projects );
        inactiveProjectSelectors.forEach( this::deactivateOptionalProject );
    }

    /**
     * Mark a project as required and activated.
     * @param selector The selector of the project.
     */
    public void activateRequiredProject( String selector )
    {
        this.activations.add( new ProjectActivationSettings( selector, ActivationSettings.ACTIVATION_REQUIRED ) );
    }

    /**
     * Mark a project as optional and activated.
     * @param selector The selector of the project.
     */
    public void activateOptionalProject( String selector )
    {
        this.activations.add( new ProjectActivationSettings( selector, ActivationSettings.ACTIVATION_OPTIONAL ) );
    }

    /**
     * Mark a project as required and deactivated.
     * @param selector The selector of the project.
     */
    public void deactivateRequiredProject( String selector )
    {
        this.activations.add( new ProjectActivationSettings( selector, ActivationSettings.DEACTIVATION_REQUIRED ) );
    }

    /**
     * Mark a project as optional and deactivated.
     * @param selector The selector of the project.
     */
    public void deactivateOptionalProject( String selector )
    {
        this.activations.add( new ProjectActivationSettings( selector, ActivationSettings.DEACTIVATION_OPTIONAL ) );
    }

    public boolean isEmpty()
    {
        return this.activations.isEmpty();
    }
}
