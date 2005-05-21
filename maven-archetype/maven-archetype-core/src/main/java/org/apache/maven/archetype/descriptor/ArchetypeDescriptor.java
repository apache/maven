package org.apache.maven.archetype.descriptor;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

public class ArchetypeDescriptor
{
    private String id;

    private List sources;

    private List testSources;

    private List resources;

    private List testResources;

    private List siteResources;

    public ArchetypeDescriptor()
    {
        sources = new ArrayList();

        resources = new ArrayList();

        testSources = new ArrayList();

        testResources = new ArrayList();

        siteResources = new ArrayList();
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

    public void addSiteResource( String siteResource )
    {
        siteResources.add( siteResource );
    }

    public List getSiteResources()
    {
        return siteResources;
    }
}

