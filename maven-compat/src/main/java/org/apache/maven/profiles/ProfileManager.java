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

import org.apache.maven.model.Profile;
import org.apache.maven.profiles.activation.ProfileActivationException;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ProfileManager
 */
@Deprecated
public interface ProfileManager
{

    void addProfile( Profile profile );

    void explicitlyActivate( String profileId );

    void explicitlyActivate( List<String> profileIds );

    void explicitlyDeactivate( String profileId );

    void explicitlyDeactivate( List<String> profileIds );

    List getActiveProfiles()
        throws ProfileActivationException;

    void addProfiles( List<Profile> profiles );

    Map getProfilesById();

    List<String> getExplicitlyActivatedIds();

    List<String> getExplicitlyDeactivatedIds();

    List getIdsActivatedByDefault();

    Properties getRequestProperties();

}
