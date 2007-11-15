package org.apache.maven.profiles.activation;

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

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationCustom;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Profile activator that allows the use of custom third-party activators, by specifying a type -
 * or role-hint - for the activator, along with a configuration (in the form of a DOM) to be used
 * in configuring the activator. This activator will lookup/configure custom activators on-the-fly,
 * without caching any of the lookups from the container.
 */
public class CustomActivator
    implements ProfileActivator, Contextualizable, LogEnabled
{

    private PlexusContainer container;

    private Logger logger;

    public boolean canDetermineActivation( Profile profile, ProfileActivationContext context )
        throws ProfileActivationException
    {
        Activation activation = profile.getActivation();

        if ( activation != null )
        {
            ActivationCustom custom = activation.getCustom();

            if ( custom != null )
            {
                ProfileActivator activator = loadProfileActivator( custom, context );

                if ( activator != null )
                {
                    return activator.canDetermineActivation( profile, context );
                }
            }
        }

        return false;
    }

    private ProfileActivator loadProfileActivator( ActivationCustom custom, ProfileActivationContext context )
        throws ProfileActivationException
    {
        String type = custom.getType();

        ProfileActivator activator = null;

        try
        {
            activator = (ProfileActivator) container.lookup( ProfileActivator.ROLE, type );
        }
        catch ( ComponentLookupException e )
        {
            if ( !context.isCustomActivatorFailureSuppressed() )
            {
                throw new ProfileActivationException( "Cannot find custom ProfileActivator: " + type
                    + ". \nPerhaps you're missing a build extension?", e );
            }
        }

        PlexusConfiguration configuration = new XmlPlexusConfiguration( (Xpp3Dom) custom.getConfiguration() );

        ComponentConfigurator configurator = new BasicComponentConfigurator();

        try
        {
            configurator.configureComponent( activator, configuration, container.getContainerRealm() );
        }
        catch ( ComponentConfigurationException e )
        {
            if ( !context.isCustomActivatorFailureSuppressed() )
            {
                throw new ProfileActivationException( "Failed to configure custom ProfileActivator: " + type
                    + ".", e );
            }
        }

        return activator;
    }

    public boolean isActive( Profile profile, ProfileActivationContext context )
        throws ProfileActivationException
    {
        ActivationCustom custom = profile.getActivation().getCustom();

        ProfileActivator activator = loadProfileActivator( custom, context );

        return activator.isActive( profile, context );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "CustomActivator-instantiated" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
