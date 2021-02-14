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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toSet;

/**
 * Container for storing the request from the user to activate or de-activate certain projects and optionally fail the
 * build if those projects do not exist.
 */
public class ProjectActivation
{
    private final Map<String, ActivationSettings> activations = new HashMap<>();

    /**
     * Adds a project activation to the request.
     * @param selector The selector of the project.
     *                 This can be the project directory, [groupId]:[artifactId] or :[artifactId].
     * @param active Should the project be activated?
     * @param optional Can the build continue if the project does not exist?
     */
    public void addProjectActivation( String selector, boolean active, boolean optional )
    {
        final ActivationSettings settings = ActivationSettings.of( active, optional );
        this.activations.put( selector, settings );
    }

    private Set<String> getProjectSelectors( final Predicate<ActivationSettings> predicate )
    {
        return this.activations.entrySet().stream()
                .filter( e -> predicate.test( e.getValue() ) )
                .map( e -> e.getKey() )
                .collect( toSet() );
    }

    /**
     * @return Required active project selectors, never {@code null}.
     * The selector can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public Set<String> getRequiredActiveProjectSelectors()
    {
        return getProjectSelectors( pa -> !pa.optional && pa.active );
    }

    /**
     * @return Optional active project selectors, never {@code null}.
     * The selector can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public Set<String> getOptionalActiveProjectSelectors()
    {
        return getProjectSelectors( pa -> pa.optional && pa.active );
    }

    /**
     * @return Required inactive project selectors, never {@code null}.
     * The selector can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public Set<String> getRequiredInactiveProjectSelectors()
    {
        return getProjectSelectors( pa -> !pa.optional && !pa.active );
    }

    /**
     * @return Optional inactive project selectors, never {@code null}.
     * The selector can be the project directory, [groupId]:[artifactId] or :[artifactId].
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
        getSelectedProjects().forEach( this.activations::remove );
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
        getExcludedProjects().forEach( this.activations::remove );
        inactiveProjectSelectors.forEach( this::deactivateOptionalProject );
    }

    /**
     * Mark a project as required and activated.
     * @param selector The selector of the project.
     *                 It can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public void activateRequiredProject( String selector )
    {
        this.activations.put( selector, ActivationSettings.ACTIVATION_REQUIRED );
    }

    /**
     * Mark a project as optional and activated.
     * @param selector The selector of the project.
     *                 It can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public void activateOptionalProject( String selector )
    {
        this.activations.put( selector, ActivationSettings.ACTIVATION_OPTIONAL );
    }

    /**
     * Mark a project as required and deactivated.
     * @param selector The selector of the project.
     *                 It can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public void deactivateRequiredProject( String selector )
    {
        this.activations.put( selector, ActivationSettings.DEACTIVATION_REQUIRED );
    }

    /**
     * Mark a project as optional and deactivated.
     * @param selector The selector of the project.
     *                 It can be the project directory, [groupId]:[artifactId] or :[artifactId].
     */
    public void deactivateOptionalProject( String selector )
    {
        this.activations.put( selector, ActivationSettings.DEACTIVATION_OPTIONAL );
    }
}
