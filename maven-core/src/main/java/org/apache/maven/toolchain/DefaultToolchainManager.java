package org.apache.maven.toolchain;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * @author mkleint
 */
@Component( role = ToolchainManager.class )
public class DefaultToolchainManager
    implements ToolchainManager
{
    @Requirement
    Logger logger;

    @Requirement( role = ToolchainFactory.class )
    Map<String, ToolchainFactory> factories;

    public Toolchain getToolchainFromBuildContext( String type, MavenSession session )
    {
        Map<String, Object> context = retrieveContext( session );

        ToolchainModel model = (ToolchainModel) context.get( getStorageKey( type ) );

        if ( model != null )
        {
            try
            {
                ToolchainFactory fact = factories.get( type );
                if ( fact != null )
                {
                    return fact.createToolchain( model );
                }
                else
                {
                    logger.error( "Missing toolchain factory for type: " + type
                        + ". Possibly caused by misconfigured project." );
                }
            }
            catch ( MisconfiguredToolchainException ex )
            {
                logger.error( "Misconfigured toolchain.", ex );
            }
        }

        return null;
    }

    Map<String, Object> retrieveContext( MavenSession session )
    {
        Map<String, Object> context = null;

        if ( session != null )
        {
            PluginDescriptor desc = new PluginDescriptor();
            desc.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
            desc.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( "toolchains" ) );

            MavenProject current = session.getCurrentProject();
            
            if ( current != null )
            {
                //TODO: why is this using the context
                context = session.getPluginContext( desc, current );
            }
        }

        return ( context != null ) ? context : new HashMap<String, Object>();
    }

    public static final String getStorageKey( String type )
    {
        return "toolchain-" + type; // NOI18N
    }

}
