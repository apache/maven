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
        List restrictions;
        if ( r1.isEmpty() )
        {
            restrictions = r2;
        }
        else if ( r2.isEmpty() )
        {
            restrictions = r1;
        }
        else
        {
            restrictions = intersection( r1, r2 );
        }

        if ( version != null && restrictions.size() > 0 )
        {
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

    private List intersection( List r1, List r2 )
    {
        List restrictions = new ArrayList( r1.size() + r2.size() );
        Iterator i1 = r1.iterator();
        Iterator i2 = r2.iterator();
        Restriction res1 = (Restriction) i1.next();
        Restriction res2 = (Restriction) i2.next();

        boolean done = false;
        while ( !done )
        {
            if ( res1.getLowerBound() == null || res2.getUpperBound() == null ||
                res1.getLowerBound().compareTo( res2.getUpperBound() ) <= 0 )
            {
                if ( res1.getUpperBound() == null || res2.getLowerBound() == null ||
                    res1.getUpperBound().compareTo( res2.getLowerBound() ) >= 0 )
                {
                    ArtifactVersion lower;
                    ArtifactVersion upper;
                    boolean lowerInclusive;
                    boolean upperInclusive;

                    // overlaps
                    if ( res1.getLowerBound() == null )
                    {
                        lower = res2.getLowerBound();
                        lowerInclusive = res2.isLowerBoundInclusive();
                    }
                    else if ( res2.getLowerBound() == null )
                    {
                        lower = res1.getLowerBound();
                        lowerInclusive = res1.isLowerBoundInclusive();
                    }
                    else
                    {
                        int comparison = res1.getLowerBound().compareTo( res2.getLowerBound() );
                        if ( comparison < 0 )
                        {
                            lower = res2.getLowerBound();
                            lowerInclusive = res2.isLowerBoundInclusive();
                        }
                        else if ( comparison == 0 )
                        {
                            lower = res1.getLowerBound();
                            lowerInclusive = res1.isLowerBoundInclusive() && res2.isLowerBoundInclusive();
                        }
                        else
                        {
                            lower = res1.getLowerBound();
                            lowerInclusive = res1.isLowerBoundInclusive();
                        }
                    }

                    if ( res1.getUpperBound() == null )
                    {
                        upper = res2.getUpperBound();
                        upperInclusive = res2.isUpperBoundInclusive();
                    }
                    else if ( res2.getUpperBound() == null )
                    {
                        upper = res1.getUpperBound();
                        upperInclusive = res1.isUpperBoundInclusive();
                    }
                    else
                    {
                        int comparison = res1.getUpperBound().compareTo( res2.getUpperBound() );
                        if ( comparison < 0 )
                        {
                            upper = res1.getUpperBound();
                            upperInclusive = res1.isUpperBoundInclusive();
                        }
                        else if ( comparison == 0 )
                        {
                            upper = res1.getUpperBound();
                            upperInclusive = res1.isUpperBoundInclusive() && res2.isUpperBoundInclusive();
                        }
                        else
                        {
                            upper = res2.getUpperBound();
                            upperInclusive = res2.isUpperBoundInclusive();
                        }
                    }

                    // don't add if they are equal and one is not inclusive
                    if ( lower == null || upper == null || lower.compareTo( upper ) != 0 )
                    {
                        restrictions.add( new Restriction( lower, lowerInclusive, upper, upperInclusive ) );
                    }
                    else if ( lowerInclusive && upperInclusive )
                    {
                        restrictions.add( new Restriction( lower, lowerInclusive, upper, upperInclusive ) );
                    }

                    //noinspection ObjectEquality
                    if ( upper == res2.getUpperBound() )
                    {
                        // advance res2
                        if ( i2.hasNext() )
                        {
                            res2 = (Restriction) i2.next();
                        }
                        else
                        {
                            done = true;
                        }
                    }
                    else
                    {
                        // advance res1
                        if ( i1.hasNext() )
                        {
                            res1 = (Restriction) i1.next();
                        }
                        else
                        {
                            done = true;
                        }
                    }
                }
                else
                {
                    // move on to next in r1
                    if ( i1.hasNext() )
                    {
                        res1 = (Restriction) i1.next();
                    }
                    else
                    {
                        done = true;
                    }
                }
            }
            else
            {
                // move on to next in r2
                if ( i2.hasNext() )
                {
                    res2 = (Restriction) i2.next();
                }
                else
                {
                    done = true;
                }
            }
        }

        return restrictions;
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
