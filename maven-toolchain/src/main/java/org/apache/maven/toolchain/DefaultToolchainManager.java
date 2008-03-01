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

package org.apache.maven.toolchain;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.apache.maven.toolchain.model.io.xpp3.MavenToolchainsXpp3Reader;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 *
 * @author mkleint
 */
public class DefaultToolchainManager extends AbstractLogEnabled
    implements ToolchainManager,
               ToolchainManagerPrivate,
               Contextualizable
{

    /**
     * @component
     */
    private PlexusContainer container;

    public DefaultToolchainManager( )
    {
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    public ToolchainPrivate[] getToolchainsForType( String type )
        throws MisconfiguredToolchainException
    {
        try
        {
            PersistedToolchains pers = readToolchainSettings ();
            Map factories = container.lookupMap( ToolchainFactory.ROLE );
            List toRet = new ArrayList(  );
            if ( pers != null )
            {
                List lst = pers.getToolchains();
                if ( lst != null )
                {
                    Iterator it = lst.iterator();
                    while ( it.hasNext() )
                    {
                        ToolchainModel toolchainModel = (ToolchainModel) it.next();
                        ToolchainFactory fact = (ToolchainFactory) factories.get( toolchainModel.getType() );
                        if ( fact != null )
                        {
                            toRet.add( fact.createToolchain( toolchainModel ) );
                        }
                        else
                        {
                            getLogger().error("Missing toolchain factory for type:" + toolchainModel.getType() + ". Possibly caused by misconfigured project.");
                        }
                    }
                }
            }
            Iterator it = factories.values().iterator();
            while ( it.hasNext() )
            {
                ToolchainFactory fact = (ToolchainFactory) it.next();
                ToolchainPrivate tool = fact.createDefaultToolchain();
                if ( tool != null )
                {
                    toRet.add( tool );
                }
            }
            ToolchainPrivate[] tc = new ToolchainPrivate[ toRet.size() ];
            return (ToolchainPrivate[]) toRet.toArray(tc);
        }
        catch ( ComponentLookupException ex )
        {
            getLogger().fatalError("Error in component lookup", ex);
        }
        return new ToolchainPrivate[0];
    }

    public Toolchain getToolchainFromBuildContext( String type,
                                                   MavenSession session )
    {
        Map context = retrieveContext(session);
        if ( "javac".equals( type )) 
        {
            //HACK to make compiler plugin happy
            type = "jdk";
        }
        Object obj = context.get( getStorageKey( type ) );
        ToolchainModel model = (ToolchainModel)obj;
        
        if ( model != null ) 
        {
            try
            {
                ToolchainFactory fact = (ToolchainFactory) container.lookup(ToolchainFactory.ROLE, type);
                return fact.createToolchain( model );
            }
            catch ( ComponentLookupException ex )
            {
                getLogger().fatalError("Error in component lookup", ex);
            }
            catch ( MisconfiguredToolchainException ex )
            {
                getLogger().error("Misconfigured toolchain.", ex);
            }
        }
        return null;
    }

    private MavenProject getCurrentProject(MavenSession session) {
        //use reflection since MavenSession.getCurrentProject() is not part of 3.0.8
        try 
        {
            Method meth = session.getClass().getMethod("getCurrentProject", new Class[0]);
            return (MavenProject) meth.invoke(session, null);
        } catch (Exception ex) 
        {
            //just ignore, we're running in pre- 3.0.9
        }
        return null;
    }
    
    private Map retrieveContext( MavenSession session ) 
    {
        if (session == null) 
        {
            return new HashMap();
        }
        PluginDescriptor desc = new PluginDescriptor();
        desc.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
        desc.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId ("toolchains") );
        MavenProject current = getCurrentProject(session);
        if ( current != null ) 
        {
            return session.getPluginContext( desc, current );
            
        }
        return new HashMap();
    }
    

    public void storeToolchainToBuildContext( ToolchainPrivate toolchain,
                                              MavenSession session )
    {
        Map context = retrieveContext( session );
        context.put( getStorageKey( toolchain.getType() ), toolchain.getModel () );
    }
    
    public static final String getStorageKey( String type )
    {
        return "toolchain-" + type; //NOI18N
    }
    

    private PersistedToolchains readToolchainSettings( )
        throws MisconfiguredToolchainException
    {
        //TODO how to point to the local path?
        File tch = new File( System.getProperty( "user.home" ),
            ".m2/toolchains.xml" );
        if ( tch.exists() )
        {
            MavenToolchainsXpp3Reader reader = new MavenToolchainsXpp3Reader();
            InputStreamReader in = null;
            try
            {
                in = new InputStreamReader( new BufferedInputStream( new FileInputStream( tch ) ) );
                return reader.read( in );
            }
            catch ( Exception ex )
            {
                throw new MisconfiguredToolchainException( "Cannot read toolchains file at " + tch.getAbsolutePath(  ),
                    ex );
            }
            finally
            {
                if (in != null) 
                {
                    try 
                    {
                        in.close();
                    } 
                    catch (IOException ex) 
                    { }
                }
//                IOUtil.close( in );
            }
        }
        else
        {
            //TODO log the fact that no toolchains file was found.
        }
        return null;
    }
}