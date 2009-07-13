package org.apache.maven.artifact.manager;

import java.util.HashMap;
import java.util.Map;

public class DefaultWagonProviderMapping
    implements WagonProviderMapping
{
    
    private Map<String, String> wagonProviders = new HashMap<String, String>();
    
    public String getWagonProvider( String protocol )
    {
        return wagonProviders.get( protocol );
    }

    public void setWagonProvider( String protocol, String provider )
    {
        wagonProviders.put( protocol, provider );
    }
    
    public void setWagonProviders( Map<String, String> wagonProviders )
    {
        this.wagonProviders = wagonProviders;
    }

}
