package org.apache.maven.settings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

public class RuntimeInfo
{

    private File file;
    private boolean pluginUpdateForced = false;
    
    // using Boolean for 3VL (null, true-to-all, false-to-all)
    private Boolean applyToAllPluginUpdates;
    
    private Map activeProfileToSourceLevel = new HashMap();
    
    private String localRepositorySourceLevel = TrackableBase.USER_LEVEL;
    
    private final Settings settings;
    
    public RuntimeInfo( Settings settings )
    {
        this.settings = settings;
    }
    
    public void setFile( File file )
    {
        this.file = file;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public void setPluginUpdateForced( boolean pluginUpdateForced )
    {
        this.pluginUpdateForced = pluginUpdateForced;
    }
    
    public boolean isPluginUpdateForced()
    {
        return pluginUpdateForced;
    }

    public Boolean getApplyToAllPluginUpdates()
    {
        return applyToAllPluginUpdates;
    }

    public void setApplyToAllPluginUpdates( Boolean applyToAll )
    {
        this.applyToAllPluginUpdates = applyToAll;
    }
    
    public void setActiveProfileSourceLevel( String activeProfile, String sourceLevel )
    {
        activeProfileToSourceLevel.put( activeProfile, sourceLevel );
    }
    
    public String getSourceLevelForActiveProfile( String activeProfile )
    {
        String sourceLevel = (String) activeProfileToSourceLevel.get( activeProfile );
        
        if ( sourceLevel != null )
        {
            return sourceLevel;
        }
        else
        {
            return settings.getSourceLevel();
        }
    }
    
    public void setLocalRepositorySourceLevel( String localRepoSourceLevel )
    {
        this.localRepositorySourceLevel = localRepoSourceLevel;
    }
    
    public String getLocalRepositorySourceLevel()
    {
        return localRepositorySourceLevel;
    }
    
}
