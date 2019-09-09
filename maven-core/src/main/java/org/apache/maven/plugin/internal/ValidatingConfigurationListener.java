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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final MojoDescriptor mojoDescriptor;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<String, Parameter> missingParameters;

    ValidatingConfigurationListener( Object mojo, MojoDescriptor mojoDescriptor, ConfigurationListener delegate )
    {
        this.mojo = mojo;
        this.delegate = delegate;
        this.mojoDescriptor = mojoDescriptor;
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

        if ( logger.isWarnEnabled() )
        {
            warnDeprecated( fieldName, value );
        }
    }

    private void warnDeprecated( String fieldName, Object value )
    {
        Parameter parameter = mojoDescriptor.getParameterMap().get( fieldName );
        String deprecated = parameter.getDeprecated();
        if ( deprecated != null && !deprecated.isEmpty() )
        {
            if ( !toString( value ).equals( toString( parameter.getDefaultValue() ) ) )
            {
                StringBuilder sb = new StringBuilder( "  Parameter '" );
                sb.append( fieldName ).append( '\'' );
                if ( parameter.getExpression() != null )
                {
                    String userProperty = parameter.getExpression().replace( "${", "'" ).replace( '}', '\'' );
                    sb.append( " (User Property " ).append( userProperty ).append( ")" );
                }
                sb.append( " is deprecated. " ).append( deprecated );

                logger.warn( MessageUtils.buffer().warning( sb.toString() ).toString() );
            }
        }
    }

    /**
     * Creates a human-friendly string representation of the specified object.
     *
     * @param obj The object to create a string representation for, may be <code>null</code>.
     * @return The string representation, never <code>null</code>.
     */
    private static String toString( Object obj )
    {
        String str;
        if ( obj != null && obj.getClass().isArray() )
        {
            int n = Array.getLength( obj );
            StringBuilder buf = new StringBuilder( 256 );
            StringJoiner sj = new StringJoiner( ", ", "[", "]" );
            for ( int i = 0; i < n; i++ )
            {
                sj.add( String.valueOf( Array.get( obj, i ) ) );
            }
            buf.append( sj.toString() );
            str = buf.toString();
        }
        else
        {
            str = String.valueOf( obj );
        }
        return str;
    }
}
