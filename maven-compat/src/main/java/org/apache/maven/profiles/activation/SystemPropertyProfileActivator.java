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

import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * SystemPropertyProfileActivator
 */
@Deprecated
public class SystemPropertyProfileActivator
    extends DetectedProfileActivator implements Contextualizable
{
    private Properties properties;

    public void contextualize( Context context )
        throws ContextException
    {
        properties = (Properties) context.get( "SystemProperties" );
    }

    protected boolean canDetectActivation( Profile profile )
    {
        return profile.getActivation() != null && profile.getActivation().getProperty() != null;
    }

    public boolean isActive( Profile profile )
        throws ProfileActivationException
    {
        Activation activation = profile.getActivation();

        ActivationProperty property = activation.getProperty();

        if ( property != null )
        {
            String name = property.getName();
            boolean reverseName = false;

            if ( name == null )
            {
                throw new ProfileActivationException( "The property name is required to activate the profile '"
                    + profile.getId() + "'" );
            }

            if ( name.startsWith( "!" ) )
            {
                reverseName = true;
                name = name.substring( 1 );
            }

            String sysValue = properties.getProperty( name );

            String propValue = property.getValue();
            if ( StringUtils.isNotEmpty( propValue ) )
            {
                boolean reverseValue = false;
                if ( propValue.startsWith( "!" ) )
                {
                    reverseValue = true;
                    propValue = propValue.substring( 1 );
                }

                // we have a value, so it has to match the system value...
                boolean result = propValue.equals( sysValue );

                if ( reverseValue )
                {
                    return !result;
                }
                else
                {
                    return result;
                }
            }
            else
            {
                boolean result = StringUtils.isNotEmpty( sysValue );

                if ( reverseName )
                {
                    return !result;
                }
                else
                {
                    return result;
                }
            }
        }

        return false;
    }

}
