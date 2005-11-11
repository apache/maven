package org.apache.maven.bootstrap.model;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes a resource.
 *
 * @version $Id$
 */
public class Resource
    implements Serializable
{
    private String directory;

    private List includes = new ArrayList();

    private List excludes = new ArrayList();

    public void addInclude( String pattern )
    {
        this.includes.add( pattern );
    }

    public void addExclude( String pattern )
    {
        this.excludes.add( pattern );
    }

    public List getIncludes()
    {
        return this.includes;
    }

    public List getExcludes()
    {
        return this.excludes;
    }

    public void setDirectory( String directory )
    {
        this.directory = directory;
    }

    public String getDirectory()
    {
        return this.directory;
    }
}
