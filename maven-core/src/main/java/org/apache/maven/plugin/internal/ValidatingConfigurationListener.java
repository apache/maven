package org.apache.maven.plugin.internal;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.component.configurator.ConfigurationListener;

/**
 * A configuration listener to help validate the plugin configuration. For instance, check for required but missing
 * parameters.
 *
 * @author Benjamin Bentmann
 */
class ValidatingConfigurationListener
    implements ConfigurationListener
{

    private final Object mojo;

    private final ConfigurationListener delegate;

    private final Map<String, Parameter> missingParameters;

    ValidatingConfigurationListener( Object mojo, MojoDescriptor mojoDescriptor, ConfigurationListener delegate )
    {
        this.mojo = mojo;
        this.delegate = delegate;
        this.missingParameters = new HashMap<>();

        if ( mojoDescriptor.getParameters() != null )
        {
            for ( Parameter param : mojoDescriptor.getParameters() )
            {
                if ( param.isRequired() )
                {
                    missingParameters.put( param.getName(), param );
                }
            }
        }
    }

    public Collection<Parameter> getMissingParameters()
    {
        return missingParameters.values();
    }

    public void notifyFieldChangeUsingSetter( String fieldName, Object value, Object target )
    {
        delegate.notifyFieldChangeUsingSetter( fieldName, value, target );

        if ( mojo == target )
        {
            notify( fieldName, value );
        }
    }

    public void notifyFieldChangeUsingReflection( String fieldName, Object value, Object target )
    {
        delegate.notifyFieldChangeUsingReflection( fieldName, value, target );

        if ( mojo == target )
        {
            notify( fieldName, value );
        }
    }

    private void notify( String fieldName, Object value )
    {
        if ( value != null )
        {
            missingParameters.remove( fieldName );
        }
    }

}
