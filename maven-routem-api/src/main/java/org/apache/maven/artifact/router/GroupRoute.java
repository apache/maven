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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GroupRoute
{
    
    public static final GroupRoute CENTRAL = new GroupRoute( "http://repo1.maven.org/maven2", new GroupPattern( "*" ) );

    private final String canonicalUrl;
    
    private final List<GroupPattern> groupPatterns = new ArrayList<GroupPattern>();
    
    public GroupRoute( String canonicalUrl, Collection<GroupPattern> groupPatterns )
    {
        this.canonicalUrl = canonicalUrl;
        merge( groupPatterns );
    }
    
    public GroupRoute( String canonicalUrl, GroupPattern...groupPatterns )
    {
        this.canonicalUrl = canonicalUrl;
        merge( groupPatterns );
    }
    
    public boolean merge( GroupPattern...groupPatterns )
    {
        return merge( Arrays.asList( groupPatterns ) );
    }
    
    public synchronized boolean merge( Collection<GroupPattern> groupPatterns )
    {
        boolean changed = false;
        
        if ( groupPatterns != null && !groupPatterns.isEmpty() )
        {
            List<GroupPattern> all = new ArrayList<GroupPattern>( this.groupPatterns );
            all.addAll( groupPatterns );
            Collections.sort( all );
            Collections.reverse( all );
            
            this.groupPatterns.clear();
            
            GroupPattern last = all.remove( 0 );
            for ( GroupPattern p : all )
            {
                if ( !last.implies( p ) )
                {
                    this.groupPatterns.add( last );
                    last = p;
                }
            }
            
            this.groupPatterns.add( last );
        }
        
        return changed;
    }
    
    public boolean contains( GroupPattern pattern )
    {
        for ( GroupPattern p : groupPatterns )
        {
            if ( p.implies( pattern ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean contains( String pattern )
    {
        for ( GroupPattern p : groupPatterns )
        {
            if ( p.implies( pattern ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    public String getCanonicalUrl()
    {
        return canonicalUrl;
    }
    
    public Collection<GroupPattern> getGroupPatterns()
    {
        return groupPatterns;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( canonicalUrl == null ) ? 0 : canonicalUrl.hashCode() );
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
        GroupRoute other = (GroupRoute) obj;
        if ( canonicalUrl == null )
        {
            if ( other.canonicalUrl != null )
                return false;
        }
        else if ( !canonicalUrl.equals( other.canonicalUrl ) )
            return false;
        return true;
    }

}
