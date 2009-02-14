package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides utility methods for plugin management.
 * 
 * @author Benjamin Bentmann
 */
class PluginUtils
{

    /**
     * Creates a lookup key for the specified plugin.
     * 
     * @param plugin The plugin, must not be <code>null</code>.
     * @return The lookup key for the plugin in the form groupId:artifactId:baseVersion, never <code>null</code>.
     */
    public static String constructVersionedKey( Plugin plugin )
    {
        return constructVersionedKey( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
    }

    /**
     * Creates a lookup key for the specified plugin.
     * 
     * @param plugin The plugin's descriptor, must not be <code>null</code>.
     * @return The lookup key for the plugin in the form groupId:artifactId:baseVersion, never <code>null</code>.
     */
    public static String constructVersionedKey( PluginDescriptor plugin )
    {
        return constructVersionedKey( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
    }

    /**
     * Creates a lookup key from the specified plugin coordinates.
     * 
     * @param groupId The group id of the plugin, must not be <code>null</code> or empty.
     * @param artifactId The artifact id of the plugin, must not be <code>null</code> or empty.
     * @param version The (possibly) timestamped version of the plugin, must not be <code>null</code> or empty.
     * @return The lookup key for the plugin in the form groupId:artifactId:baseVersion, never <code>null</code>.
     */
    private static String constructVersionedKey( String groupId, String artifactId, String version )
    {
        if ( StringUtils.isEmpty( version ) )
        {
            throw new IllegalStateException( "version for plugin " + groupId + ":" + artifactId + " is not set" );
        }

        String baseVersion = ArtifactUtils.toSnapshotVersion( version );

        StringBuffer key = new StringBuffer( 128 );
        key.append( groupId ).append( ':' ).append( artifactId ).append( ':' ).append( baseVersion );
        return key.toString();
    }

}
