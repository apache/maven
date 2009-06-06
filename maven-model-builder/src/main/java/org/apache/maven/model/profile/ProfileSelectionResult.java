package org.apache.maven.model.profile;

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
import java.util.List;

import org.apache.maven.model.Profile;

/**
 * Collects the results of the profile selector.
 * 
 * @author Benjamin Bentmann
 */
public class ProfileSelectionResult
{

    private List<Profile> activeProfiles;

    private List<ProfileActivationException> activationExceptions;

    public ProfileSelectionResult()
    {
        activeProfiles = new ArrayList<Profile>();
        activationExceptions = new ArrayList<ProfileActivationException>();
    }

    /**
     * Gets the profiles that have been activated.
     * 
     * @return The profiles that have been activated, never {@code null}.
     */
    public List<Profile> getActiveProfiles()
    {
        return this.activeProfiles;
    }

    /**
     * Sets the profiles that have been activated.
     * 
     * @param activeProfiles The profiles that have been activated, may be {@code null}.
     * @return This result, never {@code null}.
     */
    public ProfileSelectionResult setActiveProfiles( List<Profile> activeProfiles )
    {
        this.activeProfiles.clear();
        if ( activeProfiles != null )
        {
            this.activeProfiles.addAll( activeProfiles );
        }

        return this;
    }

    /**
     * Gets the exceptions that have occurred during profile activation.
     * 
     * @return The exceptions that have occurred during profile activation, never {@code null}.
     */
    public List<ProfileActivationException> getActivationExceptions()
    {
        return activationExceptions;
    }

    /**
     * Sets the exceptions that have occurred during profile activation.
     * 
     * @param activationExceptions The exceptions that have occurred during profile activation, may be {@code null}.
     * @return This result, never {@code null}.
     */
    public ProfileSelectionResult setActivationExceptions( List<ProfileActivationException> activationExceptions )
    {
        this.activationExceptions.clear();
        if ( activationExceptions != null )
        {
            this.activationExceptions.addAll( activationExceptions );
        }

        return this;
    }

}
