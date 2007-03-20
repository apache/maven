package org.apache.maven.execution;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.settings.Settings;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Build context object that supplies information about how Maven was invoked, including all of the
 * information available in the MavenExecutionRequest (in read-only form).
 */
public class SessionContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = SessionContext.class.getName();
    
    private static final String REQUEST_KEY = "request";
    
    private MavenSession session;
    
    private SessionContext()
    {
    }

    public SessionContext( MavenSession session )
    {
        this.session = session;
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    public static SessionContext read( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        SessionContext sessionContext = new SessionContext();
        
        if ( buildContext != null )
        {
            if ( !buildContext.retrieve( sessionContext ) )
            {
                return null;
            }
        }
        
        return sessionContext;
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        buildContext.store( this );
        
        buildContextManager.storeBuildContext( buildContext );
    }

    public Map getData()
    {
        return Collections.singletonMap( REQUEST_KEY, session );
    }

    public void setData( Map data )
    {
        this.session = (MavenSession) data.get( REQUEST_KEY );
    }

    //------------------------------------------------------------
    // DELEGATED METHODS, USING DEFENSIVE COPIES WHERE POSSIBLE.
    //------------------------------------------------------------
    
    public EventDispatcher getEventDispatcher()
    {
        return session.getEventDispatcher();
    }

    public Properties getExecutionProperties()
    {
        return new Properties( session.getExecutionProperties() );
    }

    public String getExecutionRootDirectory()
    {
        return session.getExecutionRootDirectory();
    }

    public List getGoals()
    {
        return Collections.unmodifiableList( session.getGoals() );
    }

    public ArtifactRepository getLocalRepository()
    {
        return session.getLocalRepository();
    }

    public MavenExecutionRequest getRequest()
    {
        return session.getRequest();
    }

    public Settings getSettings()
    {
        return session.getSettings();
    }

    public List getSortedProjects()
    {
        return Collections.unmodifiableList( session.getSortedProjects() );
    }

    public Date getStartTime()
    {
        return new Date( session.getStartTime().getTime() );
    }

    public boolean isUsingPOMsFromFilesystem()
    {
        return session.isUsingPOMsFromFilesystem();
    }

    public MavenSession getSession()
    {
        return session;
    }
}
