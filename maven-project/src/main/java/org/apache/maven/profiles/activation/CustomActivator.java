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

import org.apache.maven.context.BuildContextManager;
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

    private BuildContextManager buildContextManager;

    public boolean canDetermineActivation( Profile profile )
        throws ProfileActivationException
    {
        Activation activation = profile.getActivation();

        if ( activation != null )
        {
            ActivationCustom custom = activation.getCustom();

            if ( custom != null )
            {
                ProfileActivator activator = loadProfileActivator( custom );
                
                if ( activator != null )
                {
                    return activator.canDetermineActivation( profile );
                }
            }
        }

        return false;
    }

    private ProfileActivator loadProfileActivator( ActivationCustom custom )
        throws ProfileActivationException
    {
        CustomActivatorAdvice advice = CustomActivatorAdvice.getCustomActivatorAdvice( buildContextManager );

        String type = custom.getType();

        ProfileActivator activator = null;

        try
        {
            activator = (ProfileActivator) container.lookup( ProfileActivator.ROLE, type );
        }
        catch ( ComponentLookupException e )
        {
            if ( !advice.failQuietly() )
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
            if ( !advice.failQuietly() )
            {
                throw new ProfileActivationException( "Failed to configure custom ProfileActivator: " + type
                    + ".", e );
            }
        }

        return activator;
    }

    public boolean isActive( Profile profile )
        throws ProfileActivationException
    {
        ActivationCustom custom = profile.getActivation().getCustom();

        ProfileActivator activator = loadProfileActivator( custom );

        return activator.isActive( profile );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    private Logger getLogger()
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
