package org.apache.maven.artifact.resolver;

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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactResolutionResult
{
    private Set resolutionNodes;

    // calculated.
    private Set artifacts;
    
    public ArtifactResolutionResult()
    {
    }

    public Set getArtifacts()
    {
        if ( artifacts == null )
        {
            artifacts = new HashSet();
            
            for ( Iterator it = resolutionNodes.iterator(); it.hasNext(); )
            {
                ResolutionNode node = (ResolutionNode) it.next();
                artifacts.add( node.getArtifact() );
            }
        }
        
        return artifacts;
    }
    
    public Set getArtifactResolutionNodes()
    {
        return resolutionNodes;
    }

    public void setArtifactResolutionNodes( Set resolutionNodes )
    {
        this.resolutionNodes = resolutionNodes;
        
        // clear the cache
        this.artifacts = null;
    }
}
