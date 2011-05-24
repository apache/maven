package org.apache.maven.artifact.router;

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

public final class GroupPattern
    implements Comparable<GroupPattern>
{
    
    private static final String GROUP_SEPARATOR = "[\\/.]";

    private final String pattern;
    
    private transient final String[] parts;
    
    private transient final boolean wildcard;

    private transient final String basePattern;
    
    public GroupPattern( String pattern )
    {
        this.pattern = pattern;
        if ( pattern.endsWith("*") )
        {
            wildcard = true;
            pattern = pattern.substring( 0, pattern.length() - 1 );
            if ( pattern.endsWith(".") )
            {
                pattern = pattern.substring( 0, pattern.length() - 1 );
            }
        }
        else
        {
            wildcard = false;
        }
        
        this.basePattern = pattern;
        parts = pattern.split( GROUP_SEPARATOR );
        
        if ( parts.length < 1 || parts[0] == "" )
        {
            throw new IllegalArgumentException( "Invalid groupId pattern: '" + this.pattern + "'." );
        }
    }
    
    public boolean implies( GroupPattern pattern )
    {
        return implies( pattern.parts ) && ( wildcard || !pattern.wildcard );
    }
    
    public boolean implies( String pattern )
    {
        return implies( pattern.split( GROUP_SEPARATOR ) );
    }
    
    private boolean implies( String[] patternParts )
    {
        if ( parts.length > patternParts.length )
        {
            return false;
        }
        
        for ( int i = 0; i < parts.length; i++ )
        {
            if ( !parts[i].equals( patternParts[i]) )
            {
                return false;
            }
        }
        
        if ( wildcard || parts.length == patternParts.length )
        {
            return true;
        }
        
        return false;
    }
    
    public boolean moreGeneralThan( final GroupPattern other )
    {
        if ( this.implies( other ) && !other.implies( this ) )
        {
            return true;
        }
        
        return false;
    }

    public boolean lessGeneralThan( final GroupPattern other )
    {
        if ( other.implies( this ) && !this.implies( other ) )
        {
            return true;
        }
        
        return false;
    }

    public int compareTo( GroupPattern other )
    {
        if ( other.basePattern.equals( basePattern ) )
        {
            if ( wildcard == other.wildcard )
            {
                return 0;
            }
            else if ( wildcard )
            {
                return 1;
            }
            
            return -1;
        }
        else if ( basePattern.startsWith( other.basePattern ) )
        {
            return -1;
        }
        else if ( other.basePattern.startsWith( basePattern ) )
        {
            return 1;
        }
        
        return basePattern.compareTo( other.basePattern );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( basePattern == null ) ? 0 : basePattern.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        GroupPattern other = (GroupPattern) obj;
        if ( basePattern == null )
        {
            if ( other.basePattern != null )
                return false;
        }
        else if ( !basePattern.equals( other.basePattern ) )
            return false;
        return true;
    }
    
    public String getBasePattern()
    {
        return basePattern;
    }
    
    public boolean isWildcard()
    {
        return wildcard;
    }

    public String getPattern()
    {
        return pattern;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "GroupPattern [" ).append( pattern ).append( "]" );
        return builder.toString();
    }

}
