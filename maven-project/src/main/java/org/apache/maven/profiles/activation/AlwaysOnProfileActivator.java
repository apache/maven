package org.apache.maven.profiles.activation;

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
import org.apache.maven.profiles.AlwaysOnActivation;

public class AlwaysOnProfileActivator
    implements ProfileActivator
{
    public boolean canDetermineActivation( Profile profile )
    {
        return profile.getActivation() != null && profile.getActivation() instanceof AlwaysOnActivation;
    }

    public boolean isActive( Profile profile )
    {
        return true;
    }
}
