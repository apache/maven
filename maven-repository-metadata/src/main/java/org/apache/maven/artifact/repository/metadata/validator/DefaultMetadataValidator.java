package org.apache.maven.artifact.repository.metadata.validator;

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

import java.util.Collection;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.validator.MetadataProblemCollector.Severity;

/**
 * https://maven.apache.org/ref/LATEST/maven-repository-metadata/
 *
 */
public class DefaultMetadataValidator implements MetadataValidator 
{

    @Override
    public void validate( Metadata metadata, MetadataProblemCollector problems )
    {
        validate( metadata, !metadata.getPlugins().isEmpty(), problems );
    }

    @Override
    public void validate( Metadata metadata, boolean isGroupMetadata, MetadataProblemCollector problems )
    {
        // group metadata is only supposed to contain plugins
        if ( isGroupMetadata )
        {
            for ( Plugin plugin : metadata.getPlugins() )
            {
                validateStringNotEmpty ( problems, "plugins.plugin.name", plugin.getName(), 
                        plugin.getArtifactId() );
                validateStringNotEmpty ( problems, "plugins.plugin.prefix", plugin.getPrefix(), 
                        plugin.getName() );
                validateStringNotEmpty ( problems, "plugins.plugin.artifactId", plugin.getArtifactId(), 
                        plugin.getName() );
            }
            validateNullOrEmptyCollection ( problems, "groupId", metadata.getGroupId(), null );
            validateNullOrEmptyCollection ( problems, "artifactId", metadata.getArtifactId(), null );
            validateNullOrEmptyCollection ( problems, "version", metadata.getVersion(), null );
            validateNullOrEmptyCollection ( problems, "versioning", metadata.getVersioning(), null );
        }
        else
        {
            validateStringNotEmpty ( problems, "groupId", metadata.getGroupId(), null );
            validateStringNotEmpty ( problems, "artifactId", metadata.getArtifactId(), null );
            // validateStringNotEmpty ( problems, "version", metadata.getVersion(), null ); // only for snapshots
            // TODO:, distringuish between artifactId level and version level and snapshot/release
            // according to MDO nothing is required, but at least latest and one or more versions are reasonable
            // to assume
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private static boolean validateStringNotEmpty( MetadataProblemCollector problems, String fieldName, String string,
                                            String sourceHint )
    {
        if ( !validateNotNull( problems, fieldName, string, sourceHint ) )
        {
            return false;
        }

        if ( string.length() > 0 )
        {
            return true;
        }

        addViolation( problems, Severity.ERROR, fieldName, sourceHint, "is missing" );

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNotNull( MetadataProblemCollector problems, String fieldName, Object object,
                                            String sourceHint )
    {
        if ( object != null )
        {
            return true;
        }

        addViolation( problems, Severity.ERROR, fieldName, sourceHint, "is missing" );

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNullOrEmptyCollection( MetadataProblemCollector problems, String fieldName,
            Object object, String sourceHint )
    {
        if ( object == null || ( object instanceof Collection && Collection.class.cast( object ).isEmpty() ) )
        {
            return true;
        }

        addViolation( problems, Severity.WARNING, fieldName, sourceHint, "is unused" );

        return false;
    }

    private static void addViolation( MetadataProblemCollector problems, Severity severity, String fieldName,
            String sourceHint, String message )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        buffer.append( '\'' ).append( fieldName ).append( '\'' );
        
        if ( sourceHint != null )
        {
            buffer.append( " for " ).append( sourceHint );
        }
        
        buffer.append( ' ' ).append( message );
        
        problems.add( severity, buffer.toString(), -1, -1, null );
    }
}
