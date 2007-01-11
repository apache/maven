package org.apache.maven.artifact.resolver.filter;

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

import org.apache.maven.artifact.Artifact;

import java.util.Iterator;
import java.util.List;

/**
 * Filter to include from a list of artifact patterns.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class IncludesArtifactFilter
    implements ArtifactFilter
{
    private final List patterns;

    public IncludesArtifactFilter( List patterns )
    {
        this.patterns = patterns;
    }

    public boolean include( Artifact artifact )
    {
        String id = artifact.getGroupId() + ":" + artifact.getArtifactId();

        boolean matched = false;
        for ( Iterator i = patterns.iterator(); i.hasNext() & !matched; )
        {
            // TODO: what about wildcards? Just specifying groups? versions?
            if ( id.equals( i.next() ) )
            {
                matched = true;
            }
        }
        return matched;
    }
}
