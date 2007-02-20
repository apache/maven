package org.apache.maven.execution;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Build context object that supplies information about how Maven was invoked, including all of the
 * information available in the MavenExecutionRequest (in read-only form).
 */
public class ExecutionBuildContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = ExecutionBuildContext.class.getName();
    
    private static final String REQUEST_KEY = "request";
    
    private MavenExecutionRequest request;
    
    private ExecutionBuildContext()
    {
    }

    public ExecutionBuildContext( MavenExecutionRequest request )
    {
        this.request = request;
    }

    public List getActiveProfiles()
    {
        return Collections.unmodifiableList( request.getActiveProfiles() );
    }

    public String getBaseDirectory()
    {
        return request.getBaseDirectory();
    }

    public String getGlobalChecksumPolicy()
    {
        return request.getGlobalChecksumPolicy();
    }

    public List getGoals()
    {
        return Collections.unmodifiableList( request.getGoals() );
    }

    public List getInactiveProfiles()
    {
        return Collections.unmodifiableList( request.getInactiveProfiles() );
    }

    public ArtifactRepository getLocalRepository()
    {
        return request.getLocalRepository();
    }

    public File getLocalRepositoryPath()
    {
        return request.getLocalRepositoryPath();
    }

    public int getLoggingLevel()
    {
        return request.getLoggingLevel();
    }

    public List getMirrors()
    {
        return Collections.unmodifiableList( request.getMirrors() );
    }

    public List getPluginGroups()
    {
        return Collections.unmodifiableList( request.getPluginGroups() );
    }

    public String getPomFile()
    {
        return request.getPomFile();
    }

    public List getProfiles()
    {
        return Collections.unmodifiableList( request.getProfiles() );
    }

    public Properties getProperties()
    {
        return new Properties( request.getProperties() );
    }

    public List getProxies()
    {
        return Collections.unmodifiableList( request.getProxies() );
    }

    public String getReactorFailureBehavior()
    {
        return request.getReactorFailureBehavior();
    }

    public List getServers()
    {
        return Collections.unmodifiableList( request.getServers() );
    }

    public Settings getSettings()
    {
        return request.getSettings();
    }

    public String getSettingsFile()
    {
        return request.getSettingsFile();
    }

    public Date getStartTime()
    {
        return request.getStartTime();
    }

    public TransferListener getTransferListener()
    {
        return request.getTransferListener();
    }

    public boolean isInteractiveMode()
    {
        return request.isInteractiveMode();
    }

    public boolean isNoSnapshotUpdates()
    {
        return request.isNoSnapshotUpdates();
    }

    public boolean isOffline()
    {
        return request.isOffline();
    }

    public boolean isRecursive()
    {
        return request.isRecursive();
    }

    public boolean isShowErrors()
    {
        return request.isShowErrors();
    }

    public boolean isUpdateSnapshots()
    {
        return request.isUpdateSnapshots();
    }

    public boolean isUsePluginRegistry()
    {
        return request.isUsePluginRegistry();
    }

    public boolean isUsePluginUpdateOverride()
    {
        return request.isUsePluginUpdateOverride();
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    public static ExecutionBuildContext readExecutionBuildContext( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        ExecutionBuildContext executionContext = new ExecutionBuildContext();
        
        if ( buildContext != null )
        {
            if ( !buildContext.retrieve( executionContext ) )
            {
                return null;
            }
        }
        
        return executionContext;
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        buildContext.store( this );
        
        buildContextManager.storeBuildContext( buildContext );
    }

    public Map getData()
    {
        return Collections.singletonMap( REQUEST_KEY, request );
    }

    public void setData( Map data )
    {
        this.request = (MavenExecutionRequest) data.get( REQUEST_KEY );
    }
}
