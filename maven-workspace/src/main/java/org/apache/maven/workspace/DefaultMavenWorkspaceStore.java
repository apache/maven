package org.apache.maven.workspace;

import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DefaultMavenWorkspaceStore
    implements MavenWorkspaceStore, LogEnabled
{

    private Map caches = new HashMap();
//    private Logger logger;

    public void clear()
    {
        for ( Iterator it = caches.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
//            String cacheType = (String) entry.getKey();
            Map cache = (Map) entry.getValue();

//            getLogger().debug( "Clearing workspace cache for: " + cacheType + " (" + cache.size() + " entries)" );
            cache.clear();
        }
    }

    public Map getWorkspaceCache( String cacheType )
    {
        Map result = (Map) caches.get( cacheType );

        if ( result == null )
        {
            result = new HashMap();
            initWorkspaceCache( cacheType, result );
        }

//        getLogger().debug( "Retrieving workspace cache for: " + cacheType + " (" + result.size() + " entries)" );
        return result;
    }

    public void initWorkspaceCache( String cacheType,
                           Map cache )
    {
//        getLogger().debug( "Initializing workspace cache for: " + cacheType + " (" + cache.size() + " entries)" );
        caches.put( cacheType, cache );
    }

//    protected Logger getLogger()
//    {
//        if ( logger == null )
//        {
//            logger = new ConsoleLogger ( Logger.LEVEL_INFO, "internal" );
//        }
//
//        return logger;
//    }

    public void enableLogging( Logger logger )
    {
//        this.logger = logger;
    }

}
