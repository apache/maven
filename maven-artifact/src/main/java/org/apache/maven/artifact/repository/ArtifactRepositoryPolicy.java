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

    public static final String DEFAULT_CHECKSUM_POLICY = CHECKSUM_POLICY_FAIL;

    private boolean enabled;

    private String updatePolicy;

    private String checksumPolicy;

    public ArtifactRepositoryPolicy()
    {
        this( true, null, null );
    }

    public ArtifactRepositoryPolicy( final ArtifactRepositoryPolicy policy )
    {
        this( policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy() );
    }

    public ArtifactRepositoryPolicy( final boolean enabled, String updatePolicy, String checksumPolicy )
    {
        this.enabled = enabled;

        if ( updatePolicy == null )
        {
            updatePolicy = UPDATE_POLICY_DAILY;
        }
        this.updatePolicy = updatePolicy;

        if ( checksumPolicy == null )
        {
            checksumPolicy = DEFAULT_CHECKSUM_POLICY;
        }
        this.checksumPolicy = checksumPolicy;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public void setUpdatePolicy( final String updatePolicy )
    {
        if ( updatePolicy != null )
        {
            this.updatePolicy = updatePolicy;
        }
    }

    public void setChecksumPolicy( final String checksumPolicy )
    {
        if ( checksumPolicy != null )
        {
            this.checksumPolicy = checksumPolicy;
        }
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

    public boolean checkOutOfDate( final Date lastModified )
    {
        boolean checkForUpdates = false;

        if ( UPDATE_POLICY_ALWAYS.equals( updatePolicy ) )
        {
            checkForUpdates = true;
        }
        else if ( UPDATE_POLICY_DAILY.equals( updatePolicy ) )
        {
            // Get local midnight boundary
            final Calendar cal = Calendar.getInstance();

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
            final String s = updatePolicy.substring( UPDATE_POLICY_INTERVAL.length() + 1 );
            final int minutes = Integer.parseInt( s );
            final Calendar cal = Calendar.getInstance();
            cal.add( Calendar.MINUTE, -minutes );
            if ( cal.getTime().after( lastModified ) )
            {
                checkForUpdates = true;
            }
        }
        // else assume "never"
        return checkForUpdates;
    }

    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder( 64 );
        buffer.append( "{enabled=" );
        buffer.append( enabled );
        buffer.append( ", checksums=" );
        buffer.append( checksumPolicy );
        buffer.append( ", updates=" );
        buffer.append( updatePolicy );
        buffer.append( '}' );
        return buffer.toString();
    }

    public void merge( final ArtifactRepositoryPolicy policy )
    {
        if ( policy != null && policy.isEnabled() )
        {
            setEnabled( true );

            if ( ordinalOfCksumPolicy( policy.getChecksumPolicy() ) < ordinalOfCksumPolicy( getChecksumPolicy() ) )
            {
                setChecksumPolicy( policy.getChecksumPolicy() );
            }

            if ( ordinalOfUpdatePolicy( policy.getUpdatePolicy() ) < ordinalOfUpdatePolicy( getUpdatePolicy() ) )
            {
                setUpdatePolicy( policy.getUpdatePolicy() );
            }
        }
    }

    private int ordinalOfCksumPolicy( final String policy )
    {
        if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals( policy ) )
        {
            return 2;
        }
        else if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals( policy ) )
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private int ordinalOfUpdatePolicy( final String policy )
    {
        if ( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY.equals( policy ) )
        {
            return 1440;
        }
        else if ( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( policy ) )
        {
            return 0;
        }
        else if ( policy != null && policy.startsWith( ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL ) )
        {
            final String s = policy.substring( UPDATE_POLICY_INTERVAL.length() + 1 );
            return Integer.parseInt( s );
        }
        else
        {
            return Integer.MAX_VALUE;
        }
    }

}
