package org.apache.maven.context;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SystemBuildContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = SystemBuildContext.class.getName();

    private static final String SYSTEM_PROPERTIES_KEY = "system-properties";

    private static final String ENVIRONMENT_VARIABLES_KEY = "environment-variables";
    
    private Properties systemProperties;
    private Properties envars;
    
    public SystemBuildContext()
    {
        this.systemProperties = System.getProperties();
    }
    
    public void setEnvironmentVariables( Properties envars )
    {
        this.envars = envars;
    }
    
    public Properties getEnvironmentVariables()
    {
        return envars;
    }
    
    public void setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
    }
    
    public Properties getSystemProperties()
    {
        return systemProperties;
    }
    
    public String getSystemProperty( String name )
    {
        return systemProperties == null ? null : systemProperties.getProperty( name );
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    public static SystemBuildContext getSystemBuildContext( BuildContextManager buildContextManager, boolean create )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        SystemBuildContext systemContext = new SystemBuildContext();
        
        if ( buildContext != null )
        {
            SystemBuildContext ctx = new SystemBuildContext();
            
            if ( buildContext.retrieve( ctx ) || create )
            {
                systemContext = ctx;
            }
        }
        
        if ( create && systemContext == null )
        {
            systemContext = new SystemBuildContext();
        }
        
        return systemContext;
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        buildContext.store( this );
        
        buildContextManager.storeBuildContext( buildContext );
    }

    public Map getData()
    {
        Map data = new HashMap( 2 );
        data.put( SYSTEM_PROPERTIES_KEY, systemProperties );
        data.put( ENVIRONMENT_VARIABLES_KEY, envars );
        
        return data;
    }

    public void setData( Map data )
    {
        this.systemProperties = (Properties) data.get( SYSTEM_PROPERTIES_KEY );
        this.envars = (Properties) data.get( ENVIRONMENT_VARIABLES_KEY );
    }
}
