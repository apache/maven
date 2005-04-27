package org.apache.maven.plugin;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public interface PluginManager
{
    String ROLE = PluginManager.class.getName();

    void executeMojo( MavenSession session, MojoDescriptor mojoDescriptor )
        throws PluginExecutionException, PluginManagerException, ArtifactResolutionException;

    MojoDescriptor getMojoDescriptor( String goalId );

    void verifyPluginForGoal( String goalName, MavenSession session )
        throws ArtifactResolutionException, PluginManagerException;

    void verifyPlugin( String groupId, String artifactId, MavenSession session )
        throws ArtifactResolutionException, PluginManagerException;

    PluginDescriptor getPluginDescriptor( String groupId, String artifactId );
}