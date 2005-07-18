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

import java.util.StringTokenizer;

/**
 * Default implementation of artifact versioning.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultArtifactVersion
    implements ArtifactVersion
{
    private Integer majorVersion;

    private Integer minorVersion;

    private Integer incrementalVersion;

    private Integer buildNumber;

    private String qualifier;

    public DefaultArtifactVersion( String version )
    {
        parseVersion( version );
    }

    public int compareTo( Object o )
    {
        DefaultArtifactVersion otherVersion = (DefaultArtifactVersion) o;

        int result = compareIntegers( majorVersion, otherVersion.majorVersion );
        if ( result == 0 )
        {
            result = compareIntegers( minorVersion, otherVersion.minorVersion );
        }
        if ( result == 0 )
        {
            result = compareIntegers( incrementalVersion, otherVersion.incrementalVersion );
        }
        if ( result == 0 )
        {
            if ( buildNumber != null || otherVersion.buildNumber != null )
            {
                result = compareIntegers( buildNumber, otherVersion.buildNumber );
            }
            else if ( qualifier != null )
            {
                if ( otherVersion.qualifier != null )
                {
                    if ( qualifier.length() > otherVersion.qualifier.length() &&
                        qualifier.startsWith( otherVersion.qualifier ) )
                    {
                        // here, the longer one that otherwise match is considered older
                        result = -1;
                    }
                    else if ( qualifier.length() < otherVersion.qualifier.length() &&
                        otherVersion.qualifier.startsWith( qualifier ) )
                    {
                        // here, the longer one that otherwise match is considered older
                        result = 1;
                    }
                    else
                    {
                        result = qualifier.compareTo( otherVersion.qualifier );
                    }
                }
                else
                {
                    // otherVersion has no qualifier but we do - that's newer
                    result = -1;
                }
            }
            else if ( otherVersion.qualifier != null )
            {
                // otherVersion has a qualifier but we don't, we're newer
                result = 1;
            }
        }
        return result;
    }

    private int compareIntegers( Integer i1, Integer i2 )
    {
        if ( i1 == i2 )
        {
            return 0;
        }
        else if ( i1 == null )
        {
            return -1;
        }
        else if ( i2 == null )
        {
            return 1;
        }
        else
        {
            return i1.intValue() - i2.intValue();
        }
    }

    public int getMajorVersion()
    {
        return majorVersion != null ? majorVersion.intValue() : 0;
    }

    public int getMinorVersion()
    {
        return minorVersion != null ? minorVersion.intValue() : 0;
    }

    public int getIncrementalVersion()
    {
        return incrementalVersion != null ? incrementalVersion.intValue() : 0;
    }

    public int getBuildNumber()
    {
        return buildNumber != null ? buildNumber.intValue() : 0;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    public void parseVersion( String version )
    {
        int index = version.indexOf( "-" );

        String part1 = null;
        String part2 = null;

        if ( index < 0 )
        {
            part1 = version;
        }
        else
        {
            part1 = version.substring( 0, index );
            part2 = version.substring( index + 1 );
        }

        if ( part2 != null )
        {
            try
            {
                buildNumber = Integer.valueOf( part2 );
            }
            catch ( NumberFormatException e )
            {
                qualifier = part2;
            }
        }

        StringTokenizer tok = new StringTokenizer( part1, "." );
        majorVersion = Integer.valueOf( tok.nextToken() );
        if ( tok.hasMoreTokens() )
        {
            minorVersion = Integer.valueOf( tok.nextToken() );
        }
        if ( tok.hasMoreTokens() )
        {
            incrementalVersion = Integer.valueOf( tok.nextToken() );
        }
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( majorVersion );
        if ( minorVersion != null )
        {
            buf.append( "." );
            buf.append( minorVersion );
        }
        if ( incrementalVersion != null )
        {
            buf.append( "." );
            buf.append( incrementalVersion );
        }
        if ( buildNumber != null )
        {
            buf.append( "-" );
            buf.append( buildNumber );
        }
        else if ( qualifier != null )
        {
            buf.append( "-" );
            buf.append( qualifier );
        }
        return buf.toString();
    }
}
