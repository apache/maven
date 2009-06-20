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
import java.util.Properties;

/**
 * Describes the environmental context used to determine the activation status of profiles.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultProfileActivationContext
    implements ProfileActivationContext
{

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties executionProperties;

    public List<String> getActiveProfileIds()
    {
        if ( activeProfileIds == null )
        {
            activeProfileIds = new ArrayList<String>();
        }

        return activeProfileIds;
    }

    public DefaultProfileActivationContext setActiveProfileIds( List<String> activeProfileIds )
    {
        if ( activeProfileIds != null )
        {
            this.activeProfileIds = new ArrayList<String>( activeProfileIds );
        }
        else
        {
            this.activeProfileIds = null;
        }

        return this;
    }

    public List<String> getInactiveProfileIds()
    {
        if ( inactiveProfileIds == null )
        {
            inactiveProfileIds = new ArrayList<String>();
        }

        return inactiveProfileIds;
    }

    public DefaultProfileActivationContext setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        if ( inactiveProfileIds != null )
        {
            this.inactiveProfileIds = new ArrayList<String>( inactiveProfileIds );
        }
        else
        {
            this.inactiveProfileIds = null;
        }

        return this;
    }

    public Properties getExecutionProperties()
    {
        if ( executionProperties == null )
        {
            executionProperties = new Properties();
        }

        return executionProperties;
    }

    public DefaultProfileActivationContext setExecutionProperties( Properties executionProperties )
    {
        if ( executionProperties != null )
        {
            this.executionProperties = new Properties();
            this.executionProperties.putAll( executionProperties );
        }
        else
        {
            this.executionProperties = null;
        }

        return this;
    }

}
