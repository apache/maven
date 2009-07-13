package org.apache.maven.artifact.manager;

public interface WagonProviderMapping
{
    
    public void setWagonProvider( String protocol, String provider );
    
    public String getWagonProvider( String protocol );

}
