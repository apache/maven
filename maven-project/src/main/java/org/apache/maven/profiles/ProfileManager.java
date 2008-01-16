package org.apache.maven.profiles;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;

import java.util.List;
import java.util.Map;

public interface ProfileManager
{
    void addProfile( Profile profile );

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    void explicitlyActivate( String profileId );

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    void explicitlyActivate( List profileIds );

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    void explicitlyDeactivate( String profileId );

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    void explicitlyDeactivate( List profileIds );

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    void activateAsDefault( String profileId );

    ProfileActivationContext getProfileActivationContext();

    void setProfileActivationContext( ProfileActivationContext profileActivationContext );

    /**
     * @deprecated Use {@link ProfileManager#getActiveProfiles(Model)} instead.
     */
    List getActiveProfiles()
        throws ProfileActivationException;

    void addProfiles( List profiles );

    Map getProfilesById();

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    List getExplicitlyActivatedIds();

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    List getExplicitlyDeactivatedIds();

    /**
     * @deprecated Use {@link ProfileActivationContext} methods instead.
     */
    List getIdsActivatedByDefault();

    List getActiveProfiles( Model model )
        throws ProfileActivationException;
}