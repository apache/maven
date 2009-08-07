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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author mkleint
 */
@Component( role = ToolchainManagerPrivate.class )
public class DefaultToolchainManagerPrivate
    extends DefaultToolchainManager
    implements ToolchainManagerPrivate
{

    @Requirement
    private ToolchainsBuilder toolchainsBuilder;

    public ToolchainPrivate[] getToolchainsForType( String type, MavenSession context )
        throws MisconfiguredToolchainException
    {
        PersistedToolchains pers = toolchainsBuilder.build( context.getRequest().getUserToolchainsFile() );

        List<ToolchainPrivate> toRet = new ArrayList<ToolchainPrivate>();

        if ( pers != null )
        {
            List<ToolchainModel> lst = pers.getToolchains();
            if ( lst != null )
            {
                for ( ToolchainModel toolchainModel : lst )
                {
                    ToolchainFactory fact = factories.get( toolchainModel.getType() );
                    if ( fact != null )
                    {
                        toRet.add( fact.createToolchain( toolchainModel ) );
                    }
                    else
                    {
                        logger.error( "Missing toolchain factory for type: " + toolchainModel.getType()
                            + ". Possibly caused by misconfigured project." );
                    }
                }
            }
        }

        for ( ToolchainFactory toolchainFactory : factories.values() )
        {
            ToolchainPrivate tool = toolchainFactory.createDefaultToolchain();
            if ( tool != null )
            {
                toRet.add( tool );
            }
        }

        return toRet.toArray( new ToolchainPrivate[toRet.size()] );
    }

    public void storeToolchainToBuildContext( ToolchainPrivate toolchain, MavenSession session )
    {
        Map<String, Object> context = retrieveContext( session );
        context.put( getStorageKey( toolchain.getType() ), toolchain.getModel() );
    }

}
