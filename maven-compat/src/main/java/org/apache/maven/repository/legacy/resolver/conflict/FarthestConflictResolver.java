package org.apache.maven.repository.legacy.resolver.conflict;

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

import org.apache.maven.artifact.resolver.ResolutionNode;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Resolves conflicting artifacts by always selecting the <em>farthest</em> declaration. Farthest is defined as the
 * declaration that has the most transitive steps away from the project being built.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @since 3.0
 */
@Component( role = ConflictResolver.class, hint = "farthest" )
public class FarthestConflictResolver
    implements ConflictResolver
{
    // ConflictResolver methods -----------------------------------------------

    /*
    * @see org.apache.maven.artifact.resolver.conflict.ConflictResolver#resolveConflict(org.apache.maven.artifact.resolver.ResolutionNode,
    *      org.apache.maven.artifact.resolver.ResolutionNode)
    */

    public ResolutionNode resolveConflict( ResolutionNode node1, ResolutionNode node2 )
    {
        return node1.getDepth() >= node2.getDepth() ? node1 : node2;
    }
}
