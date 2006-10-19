package org.apache.maven.it0047;

import org.codehaus.plexus.PlexusTestCase;

public class Person
  extends PlexusTestCase
{
    private String name;
    
    public void setName( String name )
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
}
