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

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * A conflict resolver factory that obtains instances from a plexus container.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * TODO you don't need the container in here with the active maps (jvz).
 * @since 3.0
 */
@Component( role = ConflictResolverFactory.class )
public class DefaultConflictResolverFactory
    implements ConflictResolverFactory, Contextualizable
{
    // fields -----------------------------------------------------------------

    /**
     * The plexus container used to obtain instances from.
     */
    @Requirement
    private PlexusContainer container;

    // ConflictResolverFactory methods ----------------------------------------

    /*
    * @see org.apache.maven.artifact.resolver.conflict.ConflictResolverFactory#getConflictResolver(java.lang.String)
    */

    public ConflictResolver getConflictResolver( String type )
        throws ConflictResolverNotFoundException
    {
        try
        {
            return (ConflictResolver) container.lookup( ConflictResolver.ROLE, type );
        }
        catch ( ComponentLookupException exception )
        {
            throw new ConflictResolverNotFoundException( "Cannot find conflict resolver of type: " + type );
        }
    }

    // Contextualizable methods -----------------------------------------------

    /*
     * @see org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable#contextualize(org.codehaus.plexus.context.Context)
     */

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
