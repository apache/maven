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

import org.apache.maven.context.SystemBuildContext;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;

public class SystemPropertyProfileActivator
    extends DetectedProfileActivator 
    implements LogEnabled
{
    private Logger logger;
    
    protected boolean canDetectActivation( Profile profile )
    {
        return profile.getActivation() != null && profile.getActivation().getProperty() != null;
    }

    public boolean isActive( Profile profile )
    {
        Activation activation = profile.getActivation();

        ActivationProperty property = activation.getProperty();

        if ( property != null )
        {
            SystemBuildContext systemBuildContext = SystemBuildContext.getSystemBuildContext( getBuildContextManager(), true );
            
            String name = property.getName();
            boolean reverseName = false;
            
            if ( name.startsWith("!") )
            {
                reverseName = true;
                name = name.substring( 1 );
            }
            
            String sysValue = systemBuildContext.getSystemProperty( name );

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
    
    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "SystemPropertyProfileActivator:internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
