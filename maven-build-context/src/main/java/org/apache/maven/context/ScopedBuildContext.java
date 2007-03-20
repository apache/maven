package org.apache.maven.context;

import java.util.HashMap;
import java.util.Map;

public class ScopedBuildContext
    implements BuildContext, ManagedBuildData
{
    
    private final BuildContext parentBuildContext;
    private final String scopeKey;
    
    private Map localContext;

    public ScopedBuildContext( String scopeKey, BuildContext parentBuildContext )
    {
        this.scopeKey = scopeKey;
        this.parentBuildContext = parentBuildContext;
        
        this.localContext = (Map) parentBuildContext.get( scopeKey );
        if ( localContext == null )
        {
            this.localContext = new HashMap();
            parentBuildContext.store( this );
        }
    }

    public Object delete( Object key )
    {
        return localContext.remove( key );
    }

    public Object get( Object key )
    {
        return localContext.get( key );
    }

    public void put( Object key, Object value )
    {
        localContext.put( key, value );
    }

    /**
     * @deprecated Use {@link BuildContext#store(ManagedBuildData)} instead.
     */
    public void put( ManagedBuildData managedData )
    {
        localContext.put( managedData.getStorageKey(), managedData.getData() );
    }

    public boolean retrieve( ManagedBuildData managedData )
    {
        Map data = (Map) localContext.get( managedData.getStorageKey() );
        
        if ( data != null )
        {
            managedData.setData( data );
            return true;
        }
        else
        {
            return false;
        }
    }

    public void store( ManagedBuildData managedData )
    {
        localContext.put( managedData.getStorageKey(), managedData.getData() );
    }

    public Map getData()
    {
        return localContext;
    }

    public String getStorageKey()
    {
        return scopeKey;
    }

    public void setData( Map data )
    {
        this.localContext = data;
    }
    
    public void storeContext( BuildContextManager buildContextManager )
    {
        if ( parentBuildContext instanceof ScopedBuildContext )
        {
            ((ScopedBuildContext) parentBuildContext).storeContext( buildContextManager );
        }
        else
        {
            buildContextManager.storeBuildContext( parentBuildContext );
        }
    }
    
    public BuildContext getParentBuildContext()
    {
        return parentBuildContext;
    }
    
    public void delete( BuildContextManager buildContextManager )
    {
        parentBuildContext.delete( scopeKey );
        storeContext( buildContextManager );
    }

}
