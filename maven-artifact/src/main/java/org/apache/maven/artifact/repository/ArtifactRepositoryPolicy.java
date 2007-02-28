package org.apache.maven.artifact.repository;

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

import java.util.Calendar;
import java.util.Date;

/**
 * Describes a set of policies for a repository to use under certain conditions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ArtifactRepositoryPolicy
{
    public static final String UPDATE_POLICY_NEVER = "never";

    public static final String UPDATE_POLICY_ALWAYS = "always";

    public static final String UPDATE_POLICY_DAILY = "daily";

    public static final String UPDATE_POLICY_INTERVAL = "interval";

    public static final String CHECKSUM_POLICY_FAIL = "fail";

    public static final String CHECKSUM_POLICY_WARN = "warn";

    public static final String CHECKSUM_POLICY_IGNORE = "ignore";

    private boolean enabled;

    private String updatePolicy;

    private String checksumPolicy;

    public ArtifactRepositoryPolicy()
    {
        this( true, null, null );
    }

    public ArtifactRepositoryPolicy( boolean enabled, String updatePolicy, String checksumPolicy )
    {
        this.enabled = enabled;

        if ( updatePolicy == null )
        {
            updatePolicy = UPDATE_POLICY_DAILY;
        }
        this.updatePolicy = updatePolicy;

        if ( checksumPolicy == null )
        {
            checksumPolicy = CHECKSUM_POLICY_WARN;
        }
        this.checksumPolicy = checksumPolicy;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public void setUpdatePolicy( String updatePolicy )
    {
        this.updatePolicy = updatePolicy;
    }

    public void setChecksumPolicy( String checksumPolicy )
    {
        this.checksumPolicy = checksumPolicy;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public String getUpdatePolicy()
    {
        return updatePolicy;
    }

    public String getChecksumPolicy()
    {
        return checksumPolicy;
    }

    public boolean checkOutOfDate( Date lastModified )
    {
        boolean checkForUpdates = false;

        if ( UPDATE_POLICY_ALWAYS.equals( updatePolicy ) )
        {
            checkForUpdates = true;
        }
        else if ( UPDATE_POLICY_DAILY.equals( updatePolicy ) )
        {
            // Get midnight boundary
            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.HOUR_OF_DAY, 0 );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );
            if ( cal.getTime().after( lastModified ) )
            {
                checkForUpdates = true;
            }
        }
        else if ( updatePolicy.startsWith( UPDATE_POLICY_INTERVAL ) )
        {
            String s = updatePolicy.substring( UPDATE_POLICY_INTERVAL.length() + 1 );
            int minutes = Integer.valueOf( s ).intValue();
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.MINUTE, -minutes );
            if ( cal.getTime().after( lastModified ) )
            {
                checkForUpdates = true;
            }
        }
        // else assume "never"
        return checkForUpdates;
    }
}
