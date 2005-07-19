package org.apache.maven.artifact.versioning;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Construct a version range from a specification.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class VersionRange
{
    private final ArtifactVersion recommendedVersion;

    private final List restrictions;

    private VersionRange( ArtifactVersion recommendedVersion, List restrictions )
    {
        this.recommendedVersion = recommendedVersion;
        this.restrictions = restrictions;
    }

    public ArtifactVersion getRecommendedVersion()
    {
        return recommendedVersion;
    }

    public List getRestrictions()
    {
        return restrictions;
    }

    public static VersionRange createFromVersionSpec( String spec )
        throws InvalidVersionSpecificationException
    {
        List restrictions = new ArrayList();
        String process = spec;
        ArtifactVersion version = null;
        ArtifactVersion upperBound = null;
        ArtifactVersion lowerBound = null;

        while ( process.startsWith( "[" ) || process.startsWith( "(" ) )
        {
            int index1 = process.indexOf( ")" );
            int index2 = process.indexOf( "]" );

            int index = index2;
            if ( index2 < 0 || index1 < index2 )
            {
                if ( index1 >= 0 )
                {
                    index = index1;
                }
            }

            if ( index < 0 )
            {
                throw new InvalidVersionSpecificationException( "Unbounded range: " + spec );
            }

            Restriction restriction = parseRestriction( process.substring( 0, index + 1 ) );
            if ( lowerBound == null )
            {
                lowerBound = restriction.getLowerBound();
            }
            if ( upperBound != null )
            {
                if ( restriction.getLowerBound() == null || restriction.getLowerBound().compareTo( upperBound ) < 0 )
                {
                    throw new InvalidVersionSpecificationException( "Ranges overlap: " + spec );
                }
            }
            restrictions.add( restriction );
            upperBound = restriction.getUpperBound();

            process = process.substring( index + 1 ).trim();

            if ( process.length() > 0 && process.startsWith( "," ) )
            {
                process = process.substring( 1 ).trim();
            }
        }

        if ( process.length() > 0 )
        {
            if ( restrictions.size() > 0 )
            {
                throw new InvalidVersionSpecificationException(
                    "Only fully-qualified sets allowed in multiple set scenario: " + spec );
            }
            else
            {
                version = new DefaultArtifactVersion( process );
            }
        }

        return new VersionRange( version, restrictions );
    }

    private static Restriction parseRestriction( String spec )
        throws InvalidVersionSpecificationException
    {
        boolean lowerBoundInclusive = spec.startsWith( "[" );
        boolean upperBoundInclusive = spec.endsWith( "]" );

        String process = spec.substring( 1, spec.length() - 1 ).trim();

        Restriction restriction;

        int index = process.indexOf( "," );

        if ( index < 0 )
        {
            if ( !lowerBoundInclusive || !upperBoundInclusive )
            {
                throw new InvalidVersionSpecificationException( "Single version must be surrounded by []: " + spec );
            }

            ArtifactVersion version = new DefaultArtifactVersion( process );

            restriction = new Restriction( version, lowerBoundInclusive, version, upperBoundInclusive );
        }
        else
        {
            String lowerBound = process.substring( 0, index ).trim();
            String upperBound = process.substring( index + 1 ).trim();
            if ( lowerBound.equals( upperBound ) )
            {
                throw new InvalidVersionSpecificationException( "Range cannot have identical boundaries: " + spec );
            }

            ArtifactVersion lowerVersion = null;
            if ( lowerBound.length() > 0 )
            {
                lowerVersion = new DefaultArtifactVersion( lowerBound );
            }
            ArtifactVersion upperVersion = null;
            if ( upperBound.length() > 0 )
            {
                upperVersion = new DefaultArtifactVersion( upperBound );
            }

            if ( upperVersion != null && lowerVersion != null && upperVersion.compareTo( lowerVersion ) < 0 )
            {
                throw new InvalidVersionSpecificationException( "Range defies version ordering: " + spec );
            }

            restriction = new Restriction( lowerVersion, lowerBoundInclusive, upperVersion, upperBoundInclusive );
        }

        return restriction;
    }

    public static VersionRange createFromVersion( String version )
    {
        return new VersionRange( new DefaultArtifactVersion( version ), Collections.EMPTY_LIST );
    }

    public VersionRange restrict( VersionRange restriction )
    {
        ArtifactVersion version = max( recommendedVersion, restriction.getRecommendedVersion() );

        List r1 = this.restrictions;
        List r2 = restriction.restrictions;
        List restrictions = Collections.EMPTY_LIST;
        if ( !r1.isEmpty() || !r2.isEmpty() )
        {
            // TODO: amalgamate
            restrictions = new ArrayList( r1.size() + r2.size() );
            restrictions.addAll( r1 );
            restrictions.addAll( r2 );

            boolean found = false;
            for ( Iterator i = restrictions.iterator(); i.hasNext() && !found; )
            {
                Restriction r = (Restriction) i.next();

                if ( r.containsVersion( version ) )
                {
                    found = true;
                }
            }

            if ( !found )
            {
                version = null;
            }
        }

        return new VersionRange( version, restrictions );
    }

    private ArtifactVersion max( ArtifactVersion v1, ArtifactVersion v2 )
    {
        if ( v1 == null )
        {
            return v2;
        }
        if ( v2 == null )
        {
            return v1;
        }
        else if ( v1.compareTo( v2 ) > 0 )
        {
            return v1;
        }
        else
        {
            return v2;
        }
    }
}
