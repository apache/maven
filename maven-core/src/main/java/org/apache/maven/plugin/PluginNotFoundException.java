package org.apache.maven.plugin;

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

import org.apache.maven.artifact.resolver.ArtifactResolutionException;

/**
 * Exception occurring trying to resolve a plugin.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class PluginNotFoundException
    extends ArtifactResolutionException
{
    public PluginNotFoundException( String groupId, String artifactId, String version, ArtifactResolutionException e )
    {
        super( "Plugin could not be found - check that the goal name is correct", groupId, artifactId, version,
               "maven-plugin", e );
    }
}
