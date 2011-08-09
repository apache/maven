/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
    
    public static final GroupRoute DEFAULT = new GroupRoute( "http://repo1.maven.org/maven2", new GroupPattern( "*" ) );

    private final String canonicalReleasesUrl;
    
    private String canonicalSnapshotsUrl;
    
    private List<GroupPattern> groupPatterns = new ArrayList<GroupPattern>();
    
    public GroupRoute( String canonicalReleasesUrl, Collection<GroupPattern> groupPatterns )
    {
        this( canonicalReleasesUrl, null, groupPatterns );
    }
    
    public GroupRoute( String canonicalReleasesUrl, GroupPattern...groupPatterns )
    {
        this( canonicalReleasesUrl, null, groupPatterns );
    }
    
    public GroupRoute( String canonicalReleasesUrl, String canonicalSnapshotsUrl, Collection<GroupPattern> groupPatterns )
    {
        this.canonicalReleasesUrl = canonicalReleasesUrl;
        this.canonicalSnapshotsUrl = canonicalSnapshotsUrl;
        this.groupPatterns = consolidate( this.groupPatterns, groupPatterns );
    }
    
    public GroupRoute( String canonicalReleasesUrl, String canonicalSnapshotsUrl, GroupPattern...groupPatterns )
    {
        this.canonicalReleasesUrl = canonicalReleasesUrl;
        this.canonicalSnapshotsUrl = canonicalSnapshotsUrl;
        this.groupPatterns.clear();
        this.groupPatterns = consolidate( this.groupPatterns, Arrays.asList( groupPatterns ) );
    }
    
    public synchronized boolean merge( GroupRoute otherRoute )
    {
        boolean changed = false;
        
        if ( this.canonicalSnapshotsUrl == null && patternsAreEqual( otherRoute ) )
        {
            this.canonicalSnapshotsUrl = otherRoute.getCanonicalSnapshotsUrl();
        }
        
        this.groupPatterns = consolidate( this.groupPatterns, otherRoute.getGroupPatterns() );
        
        return changed;
    }
    
    private boolean patternsAreEqual( GroupRoute otherRoute )
    {
        List<GroupPattern> otherPatterns = otherRoute.getGroupPatterns();
        if ( groupPatterns.size() != otherPatterns.size() )
            return false;
        
        for( int i = 0; i< groupPatterns.size(); i++ )
        {
            if ( !groupPatterns.get( i ).equals( otherPatterns.get( i ) ) )
            {
                return false;
            }
        }
        
        return true;
    }

    private synchronized List<GroupPattern> consolidate( List<GroupPattern> myPatterns,
                                                                    Collection<GroupPattern> otherPatterns )
    {
        if ( otherPatterns == null || otherPatterns.isEmpty() )
        {
            return myPatterns;
        }
        
        List<GroupPattern> result = new ArrayList<GroupPattern>();
        
        List<GroupPattern> all = new ArrayList<GroupPattern>( myPatterns );
        all.addAll( otherPatterns );
        Collections.sort( all );
        Collections.reverse( all );
        
        GroupPattern last = all.remove( 0 );
        for ( GroupPattern p : all )
        {
            if ( !last.implies( p ) )
            {
                result.add( last );
                last = p;
            }
        }
        
        result.add( last );

        return result;
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
    
    public String getCanonicalReleasesUrl()
    {
        return canonicalReleasesUrl;
    }
    
    public String getCanonicalSnapshotsUrl()
    {
        return canonicalSnapshotsUrl;
    }
    
    public List<GroupPattern> getGroupPatterns()
    {
        return groupPatterns;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( canonicalReleasesUrl == null ) ? 0 : canonicalReleasesUrl.hashCode() );
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
        if ( canonicalReleasesUrl == null )
        {
            if ( other.canonicalReleasesUrl != null )
                return false;
        }
        else if ( !canonicalReleasesUrl.equals( other.canonicalReleasesUrl ) )
            return false;
        return true;
    }

}
