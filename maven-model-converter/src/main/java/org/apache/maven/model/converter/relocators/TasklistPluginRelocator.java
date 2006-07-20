package org.apache.maven.model.converter.relocators;

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
 * A <code>PluginRelocator</code> for the maven-tasklist-plugin.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @plexus.component role="org.apache.maven.model.converter.relocators.PluginRelocator"
 * role-hint="tasklist"
 */
public class TasklistPluginRelocator extends AbstractPluginRelocator
{
    /**
     * @see AbstractPluginRelocator#getNewArtifactId()
     */
    public String getNewArtifactId()
    {
        return "taglist-maven-plugin";
    }

    /**
     * @see AbstractPluginRelocator#getNewGroupId()
     */
    public String getNewGroupId()
    {
        return "org.codehaus.mojo";
    }

    /**
     * @see AbstractPluginRelocator#getOldArtifactId()
     */
    public String getOldArtifactId()
    {
        return "maven-tasklist-plugin";
    }
}
