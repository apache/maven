package org.apache.maven.model.converter;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

import java.util.Iterator;

/*
 * Copyright 2006 The Apache Software Foundation.
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
 * Utility class which features various methods associated with Maven model.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class ModelUtils
{
    /**
     * Try to find a build plugin in a model.
     *
     * @param model      Look for the build plugin in this model
     * @param groupId    The groupId for the build plugin to look for
     * @param artifactId The artifactId for the build plugin to look for
     * @return The requested build plugin if it exists, otherwise null
     */
    public static Plugin findBuildPlugin( Model model, String groupId, String artifactId )
    {
        if ( model.getBuild() == null || model.getBuild().getPlugins() == null )
        {
            return null;
        }

        Iterator iterator = model.getBuild().getPlugins().iterator();
        while ( iterator.hasNext() )
        {
            Plugin plugin = (Plugin) iterator.next();
            if ( plugin.getGroupId().equals( groupId ) && plugin.getArtifactId().equals( artifactId ) )
            {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Try to find a report plugin in a model.
     *
     * @param model      Look for the report plugin in this model
     * @param groupId    The groupId for the report plugin to look for
     * @param artifactId The artifactId for the report plugin to look for
     * @return The requested report plugin if it exists, otherwise null
     */
    public static ReportPlugin findReportPlugin( Model model, String groupId, String artifactId )
    {
        if ( model.getReporting() == null || model.getReporting().getPlugins() == null )
        {
            return null;
        }
        
        Iterator iterator = model.getReporting().getPlugins().iterator();
        while ( iterator.hasNext() )
        {
            ReportPlugin plugin = (ReportPlugin) iterator.next();
            if ( plugin.getGroupId().equals( groupId ) && plugin.getArtifactId().equals( artifactId ) )
            {
                return plugin;
            }
        }
        return null;
    }
}
