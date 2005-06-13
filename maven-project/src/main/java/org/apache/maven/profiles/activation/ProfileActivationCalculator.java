package org.apache.maven.profiles.activation;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

public class ProfileActivationCalculator
    extends AbstractLogEnabled
    implements Contextualizable
{

    public static final String ROLE = ProfileActivationCalculator.class.getName();

    private PlexusContainer container;

    public List calculateActiveProfiles( List profiles )
        throws ProjectBuildingException
    {
        List activators = null;
        try
        {
            activators = container.lookupList( ProfileActivator.ROLE );

            List active = new ArrayList( profiles.size() );

            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                boolean isActive = true;

                Activation activation = profile.getActivation();

                activatorLoop: for ( Iterator activatorIterator = activators.iterator(); activatorIterator.hasNext(); )
                {
                    ProfileActivator activator = (ProfileActivator) activatorIterator.next();

                    if ( activator.canDetermineActivation( profile ) )
                    {
                        if ( activator.isActive( profile ) )
                        {
                            active.add( profile );
                        }
                        else
                        {
                            break activatorLoop;
                        }
                    }
                }
            }

            return active;
        }
        catch ( ComponentLookupException e )
        {
            throw new ProjectBuildingException( "Cannot retrieve list of profile activators.", e );
        }
        finally
        {
            try
            {
                container.releaseAll( activators );
            }
            catch ( ComponentLifecycleException e )
            {
                getLogger().debug( "Error releasing profile activators - ignoring.", e );
            }
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}
