package org.apache.maven.archetype.descriptor;

import java.util.ArrayList;
import java.util.List;

public class ArchetypeDescriptor
{
    private String id;

    private List sources;

    private List testSources;

    private List resources;

    private List testResources;

    public ArchetypeDescriptor()
    {
        sources = new ArrayList();

        resources = new ArrayList();

        testSources = new ArrayList();        

        testResources = new ArrayList();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void addSource( String source )
    {
        sources.add( source );
    }

    public List getSources()
    {
        return sources;
    }

    public void addTestSource( String testSource )
    {
        testSources.add( testSource );
    }

    public List getTestSources()
    {
        return testSources;
    }

    public void addResource( String resource )
    {
        resources.add( resource );
    }

    public List getResources()
    {
        return resources;
    }

    public void addTestResource( String testResource )
    {
        testResources.add( testResource );
    }

    public List getTestResources()
    {
        return testResources;
    }
}

