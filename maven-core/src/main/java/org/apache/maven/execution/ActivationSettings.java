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

/**
 * Describes whether a target should be activated or not, and if that is required or optional.
 */
enum ActivationSettings
{
    ACTIVATION_OPTIONAL( true, true ),
    ACTIVATION_REQUIRED( true, false ),
    DEACTIVATION_OPTIONAL( false, true ),
    DEACTIVATION_REQUIRED( false, false );

    /**
     * Should the target be active?
     */
    final boolean active;
    /**
     * Should the build continue if the target is not present?
     */
    final boolean optional;

    ActivationSettings( final boolean active, final boolean optional )
    {
        this.active = active;
        this.optional = optional;
    }

    static ActivationSettings of( final boolean active, final boolean optional )
    {
        if ( optional )
        {
            return active ? ACTIVATION_OPTIONAL : DEACTIVATION_OPTIONAL;
        }
        else
        {
            return active ? ACTIVATION_REQUIRED : DEACTIVATION_REQUIRED;
        }
    }
}
