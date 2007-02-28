package org.apache.maven.plugin.registry;

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

import java.io.File;

public class RuntimeInfo
{

    private File file;

    private String autoUpdateSourceLevel;

    private String updateIntervalSourceLevel;

    private final PluginRegistry registry;

    public RuntimeInfo( PluginRegistry registry )
    {
        this.registry = registry;
    }

    public String getAutoUpdateSourceLevel()
    {
        if ( autoUpdateSourceLevel == null )
        {
            return registry.getSourceLevel();
        }
        else
        {
            return autoUpdateSourceLevel;
        }
    }

    public void setAutoUpdateSourceLevel( String autoUpdateSourceLevel )
    {
        this.autoUpdateSourceLevel = autoUpdateSourceLevel;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public String getUpdateIntervalSourceLevel()
    {
        if ( updateIntervalSourceLevel == null )
        {
            return registry.getSourceLevel();
        }
        else
        {
            return updateIntervalSourceLevel;
        }
    }

    public void setUpdateIntervalSourceLevel( String updateIntervalSourceLevel )
    {
        this.updateIntervalSourceLevel = updateIntervalSourceLevel;
    }

}
