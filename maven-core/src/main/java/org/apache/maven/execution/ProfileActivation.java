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
 * Container for storing the request from the user to activate or de-activate certain profiles and optionally fail the
 * build if those profiles do not exist.
 */
public class ProfileActivation
{
    private final Map<String, ActivationSettings> activations = new HashMap<>();

    /**
     * Mimics the pre-Maven 4 "active profiles" list.
     * @deprecated Use {@link #getRequiredActiveProfileIds()} and {@link #getOptionalActiveProfileIds()} instead.
     */
    @Deprecated
    public List<String> getActiveProfiles()
    {
        return Collections.unmodifiableList( new ArrayList<>( getProfileIds( pa -> pa.active ) ) );
    }

    /**
     * Mimics the pre-Maven 4 "inactive profiles" list.
     * @deprecated Use {@link #getRequiredInactiveProfileIds()} and {@link #getOptionalInactiveProfileIds()} instead.
     */
    @Deprecated
    public List<String> getInactiveProfiles()
    {
        return Collections.unmodifiableList( new ArrayList<>( getProfileIds( pa -> !pa.active ) ) );
    }

    /**
     * Overwrites the active profiles based on a pre-Maven 4 "active profiles" list.
     * @param activeProfileIds A {@link List} of profile IDs that must be activated.
     * @deprecated Use {@link #activateOptionalProfile(String)} or {@link #activateRequiredProfile(String)} instead.
     */
    @Deprecated
    public void overwriteActiveProfiles( List<String> activeProfileIds )
    {
        getActiveProfiles().forEach( this.activations::remove );
        activeProfileIds.forEach( this::activateOptionalProfile );
    }

    /**
     * Overwrites the inactive profiles based on a pre-Maven 4 "inactive profiles" list.
     * @param inactiveProfileIds A {@link List} of profile IDs that must be deactivated.
     * @deprecated Use {@link #deactivateOptionalProfile(String)} or {@link #deactivateRequiredProfile(String)} instead.
     */
    @Deprecated
    public void overwriteInactiveProfiles( List<String> inactiveProfileIds )
    {
        getInactiveProfiles().forEach( this.activations::remove );
        inactiveProfileIds.forEach( this::deactivateOptionalProfile );
    }

    /**
     * Mark a profile as required and activated.
     * @param id The identifier of the profile.
     */
    public void activateRequiredProfile( String id )
    {
        this.activations.put( id, ActivationSettings.ACTIVATION_REQUIRED );
    }

    /**
     * Mark a profile as optional and activated.
     * @param id The identifier of the profile.
     */
    public void activateOptionalProfile( String id )
    {
        this.activations.put( id, ActivationSettings.ACTIVATION_OPTIONAL );
    }

    /**
     * Mark a profile as required and deactivated.
     * @param id The identifier of the profile.
     */
    public void deactivateRequiredProfile( String id )
    {
        this.activations.put( id, ActivationSettings.DEACTIVATION_REQUIRED );
    }

    /**
     * Mark a profile as optional and deactivated.
     * @param id The identifier of the profile.
     */
    public void deactivateOptionalProfile( String id )
    {
        this.activations.put( id, ActivationSettings.DEACTIVATION_OPTIONAL );
    }

    /**
     * Adds a profile activation to the request.
     * @param id The identifier of the profile.
     * @param active Should the profile be activated?
     * @param optional Can the build continue if the profile does not exist?
     */
    public void addProfileActivation( String id, boolean active, boolean optional )
    {
        final ActivationSettings settings = ActivationSettings.of( active, optional );
        this.activations.put( id, settings );
    }

    private Set<String> getProfileIds( final Predicate<ActivationSettings> predicate )
    {
        return this.activations.entrySet().stream()
                .filter( e -> predicate.test( e.getValue() ) )
                .map( e -> e.getKey() )
                .collect( toSet() );
    }

    /**
     * @return Required active profile identifiers, never {@code null}.
     */
    public Set<String> getRequiredActiveProfileIds()
    {
        return getProfileIds( pa -> !pa.optional && pa.active );
    }

    /**
     * @return Optional active profile identifiers, never {@code null}.
     */
    public Set<String> getOptionalActiveProfileIds()
    {
        return getProfileIds( pa -> pa.optional && pa.active );
    }

    /**
     * @return Required inactive profile identifiers, never {@code null}.
     */
    public Set<String> getRequiredInactiveProfileIds()
    {
        return getProfileIds( pa -> !pa.optional && !pa.active );
    }

    /**
     * @return Optional inactive profile identifiers, never {@code null}.
     */
    public Set<String> getOptionalInactiveProfileIds()
    {
        return getProfileIds( pa -> pa.optional && !pa.active );
    }
}
