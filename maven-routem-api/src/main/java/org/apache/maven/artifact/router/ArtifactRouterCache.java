package org.apache.maven.artifact.router;

import java.util.Map;
import java.util.WeakHashMap;

import org.sonatype.aether.repository.RemoteRepository;

public class ArtifactRouterCache
{
    
    public static final String SESSION_KEY = ArtifactRouterCache.class.getName();
    
    private final Map<GroupCacheKey, RemoteRepository> groupRepositories =
        new WeakHashMap<GroupCacheKey, RemoteRepository>();

    public RemoteRepository getGroupRepository( GroupRoute groupRoute, boolean snapshot )
    {
        return groupRepositories.get( new GroupCacheKey( groupRoute, snapshot ) );
    }
    
    public void setGroupRepository( GroupRoute groupRoute, boolean snapshot,
                                      RemoteRepository repo )
    {
        groupRepositories.put( new GroupCacheKey( groupRoute, snapshot ), repo );
    }
    
    public void clear()
    {
        groupRepositories.clear();
    }
    
    private static final class GroupCacheKey
    {
        private final GroupRoute route;
        private final boolean snapshot;
        
        GroupCacheKey( GroupRoute route, boolean snapshot )
        {
            this.route = route;
            this.snapshot = snapshot;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( snapshot ? 1231 : 1237 );
            result = prime * result + ( ( route == null ) ? 0 : route.hashCode() );
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
            GroupCacheKey other = (GroupCacheKey) obj;
            if ( snapshot != other.snapshot )
                return false;
            if ( route == null )
            {
                if ( other.route != null )
                    return false;
            }
            else if ( !route.equals( other.route ) )
                return false;
            return true;
        }
    }
}
