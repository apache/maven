package org.apache.maven.artifact;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Type safe enumeration for the artifact status field.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public final class ArtifactStatus
    implements Comparable
{
    /**
     * No trust - no information about status.
     */
    public static final ArtifactStatus NONE = new ArtifactStatus( "none", 0 );

    /**
     * No trust - information was generated with defaults.
     */
    public static final ArtifactStatus GENERATED = new ArtifactStatus( "generated", 1 );

    /**
     * Low trust - was converted from the Maven 1.x repository.
     */
    public static final ArtifactStatus CONVERTED = new ArtifactStatus( "converted", 2 );

    /**
     * Moderate trust - it was deployed directly from a partner.
     */
    public static final ArtifactStatus PARTNER = new ArtifactStatus( "partner", 3 );

    /**
     * Moderate trust - it was deployed directly by a user.
     */
    public static final ArtifactStatus DEPLOYED = new ArtifactStatus( "deployed", 4 );

    /**
     * Trusted, as it has had its data verified by hand.
     */
    public static final ArtifactStatus VERIFIED = new ArtifactStatus( "verified", 5 );

    private final int rank;

    private final String key;

    private static Map map = null;

    private ArtifactStatus( String key, int rank )
    {
        this.rank = rank;
        this.key = key;

        if ( map == null )
        {
            map = new HashMap();
        }
        map.put( key, this );
    }

    public static ArtifactStatus valueOf( String status )
    {
        ArtifactStatus retVal = null;

        if ( status != null )
        {
            retVal = (ArtifactStatus) map.get( status );
        }

        return retVal != null ? retVal : NONE;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final ArtifactStatus that = (ArtifactStatus) o;

        return rank == that.rank;

    }

    public int hashCode()
    {
        return rank;
    }

    public String toString()
    {
        return key;
    }

    public int compareTo( Object o )
    {
        ArtifactStatus s = (ArtifactStatus) o;
        return rank - s.rank;
    }
}
