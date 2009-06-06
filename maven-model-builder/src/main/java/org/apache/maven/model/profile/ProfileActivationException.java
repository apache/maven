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

import org.apache.maven.model.Profile;

/**
 * Signals an error in determining the activation status of a profile (e.g. bad syntax of activation condition).
 * 
 * @author Benjamin Bentmann
 */
public class ProfileActivationException
    extends Exception
{

    /**
     * The profile which raised this error, can be {@code null}.
     */
    private Profile profile;

    /**
     * Creates a new exception with specified detail message and cause for the given profile.
     * 
     * @param message The detail message, may be {@code null}.
     * @param profile The profile that caused the error, may be {@code null}.
     * @param cause The cause, may be {@code null}.
     */
    public ProfileActivationException( String message, Profile profile, Throwable cause )
    {
        super( message, cause );
        this.profile = profile;
    }

    /**
     * Creates a new exception with specified detail message for the given profile.
     * 
     * @param message The detail message, may be {@code null}.
     * @param profile The profile that caused the error, may be {@code null}.
     */
    public ProfileActivationException( String message, Profile profile )
    {
        super( message );
        this.profile = profile;
    }

    /**
     * Gets the profile that caused this error (if any).
     * 
     * @return The profile that caused this error or {@code null} if not applicable.
     */
    public Profile getProfile()
    {
        return profile;
    }

}
